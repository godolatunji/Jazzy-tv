package com.nexient.jazzy.core.data.recommendations

import com.nexient.jazzy.core.common.ApplicationScope
import com.nexient.jazzy.core.data.library.LibraryRepository
import com.nexient.jazzy.core.data.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Rebuilds the active profile's rails after a session ends, a like/list change, or when stale. */
@OptIn(FlowPreview::class)
@Singleton
class RecommendationTriggers @Inject constructor(
    private val library: LibraryRepository,
    private val profiles: ProfileRepository,
    private val recommendations: RecommendationRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            library.events.debounce(1_500).collect {
                profiles.activeProfile.first()?.let { p -> runCatching { recommendations.rebuild(p) } }
            }
        }
        scope.launch {
            profiles.activeProfile.filterNotNull().collect { p ->
                if (recommendations.isStale(p.id)) runCatching { recommendations.rebuild(p) }
            }
        }
    }
}
