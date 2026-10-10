package com.nexient.jazzy.core.data.profile

import com.nexient.jazzy.core.common.Clock
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.data.settings.PinHasher
import com.nexient.jazzy.core.data.settings.SettingKeys
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.database.app.ProfileDao
import com.nexient.jazzy.core.database.app.ProfileEntity
import com.nexient.jazzy.core.database.app.RecommendationStateDao
import com.nexient.jazzy.core.database.app.SeedGenreEntity
import com.nexient.jazzy.core.database.app.TitleSnapshotDao
import com.nexient.jazzy.core.database.cache.RecommendationCacheDao
import com.nexient.jazzy.core.model.MAX_PROFILES
import com.nexient.jazzy.core.model.MaturityLevel
import com.nexient.jazzy.core.model.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ProfileDraft(
    val name: String,
    val avatarId: Int,
    val isKids: Boolean,
    val maturity: MaturityLevel,
    val language: String = defaultLanguage(),
    val subtitleLanguage: String? = null,
    val audioLanguage: String? = null,
    val autoplayNext: Boolean = true,
)

fun defaultLanguage(): String = Locale.getDefault().let { "${it.language}-${it.country.ifEmpty { "US" }}" }

class ProfileLimitReached : IllegalStateException("A device can have at most $MAX_PROFILES profiles")
class LastProfileDeletion : IllegalStateException("The last remaining profile can't be deleted")

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val recommendationStateDao: RecommendationStateDao,
    private val recommendationCacheDao: RecommendationCacheDao,
    private val titleSnapshotDao: TitleSnapshotDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val profiles: Flow<List<Profile>> = profileDao.observeAll().map { list -> list.map { it.toModel() } }

    /** The profile currently using the app, or null at the "Who's watching?" screen. */
    val activeProfile: Flow<Profile?> = settings.appSettings.map { it.activeProfileId }.distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else profileDao.observe(id).map { it?.toModel() } }

    val activeProfileId: Flow<Long?> = activeProfile.map { it?.id }.distinctUntilChanged()

    suspend fun create(draft: ProfileDraft, seedGenres: List<String> = emptyList()): Long = withContext(io) {
        if (profileDao.count() >= MAX_PROFILES) throw ProfileLimitReached()
        val id = profileDao.insert(
            ProfileEntity(
                name = draft.name.trim().take(20), avatarId = draft.avatarId, isKids = draft.isKids,
                maturity = (if (draft.isKids && draft.maturity == MaturityLevel.ALL) MaturityLevel.OLDER_KIDS else draft.maturity).name,
                language = draft.language, subtitleLanguage = draft.subtitleLanguage, audioLanguage = draft.audioLanguage,
                pinHash = null, pinSalt = null, autoplayNext = draft.autoplayNext, createdAt = clock.nowMillis(),
            ),
        )
        if (seedGenres.isNotEmpty()) setSeedGenres(id, seedGenres)
        id
    }

    suspend fun update(id: Long, draft: ProfileDraft) = withContext(io) {
        val existing = profileDao.get(id) ?: return@withContext
        profileDao.upsert(
            existing.copy(
                name = draft.name.trim().take(20), avatarId = draft.avatarId, isKids = draft.isKids,
                maturity = draft.maturity.name, language = draft.language, subtitleLanguage = draft.subtitleLanguage,
                audioLanguage = draft.audioLanguage, autoplayNext = draft.autoplayNext,
            ),
        )
    }

    suspend fun delete(id: Long) = withContext(io) {
        if (profileDao.count() <= 1) throw LastProfileDeletion()
        profileDao.delete(id) // cascades to lists, progress, history, settings, genres (spec PR-7)
        recommendationCacheDao.clear(id)
        titleSnapshotDao.deleteUnreferenced()
        if (settings.get(SettingKeys.ACTIVE_PROFILE)?.toLongOrNull() == id) settings.put(SettingKeys.ACTIVE_PROFILE, null)
    }

    suspend fun select(id: Long?) = settings.put(SettingKeys.ACTIVE_PROFILE, id)

    suspend fun setRememberProfile(remember: Boolean) = settings.put(SettingKeys.REMEMBER_PROFILE, remember)

    suspend fun get(id: Long): Profile? = withContext(io) { profileDao.get(id)?.toModel() }

    suspend fun setPin(id: Long, pin: String?) = withContext(io) {
        val existing = profileDao.get(id) ?: return@withContext
        if (pin.isNullOrEmpty()) {
            profileDao.upsert(existing.copy(pinHash = null, pinSalt = null))
        } else {
            val salt = PinHasher.newSalt()
            profileDao.upsert(existing.copy(pinHash = PinHasher.hash(pin, salt), pinSalt = salt))
        }
    }

    suspend fun verifyPin(id: Long, pin: String): Boolean = withContext(io) {
        val p = profileDao.get(id) ?: return@withContext false
        val hash = p.pinHash ?: return@withContext true
        PinHasher.verify(pin, p.pinSalt.orEmpty(), hash)
    }

    // Parental PIN: guards profile management and leaving a Kids profile (PR-5).
    suspend fun hasParentalPin(): Boolean = settings.get(PARENTAL_HASH) != null

    suspend fun setParentalPin(pin: String?) {
        if (pin.isNullOrEmpty()) {
            settings.put(PARENTAL_HASH, null); settings.put(PARENTAL_SALT, null)
        } else {
            val salt = PinHasher.newSalt()
            settings.put(PARENTAL_SALT, salt); settings.put(PARENTAL_HASH, PinHasher.hash(pin, salt))
        }
    }

    suspend fun verifyParentalPin(pin: String): Boolean {
        val hash = settings.get(PARENTAL_HASH) ?: return true
        return PinHasher.verify(pin, settings.get(PARENTAL_SALT).orEmpty(), hash)
    }

    fun seedGenres(profileId: Long): Flow<List<String>> =
        recommendationStateDao.observeSeeds(profileId).map { rows -> rows.map { it.unifiedGenreId } }

    suspend fun setSeedGenres(profileId: Long, genres: List<String>) = withContext(io) {
        val now = clock.nowMillis()
        recommendationStateDao.replaceSeeds(profileId, genres.distinct().map { SeedGenreEntity(profileId, it, now) })
    }

    private companion object {
        const val PARENTAL_HASH = "parental_pin_hash"
        const val PARENTAL_SALT = "parental_pin_salt"
    }
}

internal fun ProfileEntity.toModel() = Profile(
    id = id,
    name = name,
    avatarId = avatarId,
    isKids = isKids,
    maturity = runCatching { MaturityLevel.valueOf(maturity) }.getOrDefault(MaturityLevel.ALL),
    language = language,
    subtitleLanguage = subtitleLanguage,
    audioLanguage = audioLanguage,
    hasPin = pinHash != null,
    autoplayNext = autoplayNext,
    createdAt = createdAt,
)
