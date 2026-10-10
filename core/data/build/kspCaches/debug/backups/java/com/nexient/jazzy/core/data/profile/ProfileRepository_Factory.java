package com.nexient.jazzy.core.data.profile;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.data.settings.SettingsRepository;
import com.nexient.jazzy.core.database.app.ProfileDao;
import com.nexient.jazzy.core.database.app.RecommendationStateDao;
import com.nexient.jazzy.core.database.app.TitleSnapshotDao;
import com.nexient.jazzy.core.database.cache.RecommendationCacheDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nexient.jazzy.core.common.IoDispatcher")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<ProfileDao> profileDaoProvider;

  private final Provider<RecommendationStateDao> recommendationStateDaoProvider;

  private final Provider<RecommendationCacheDao> recommendationCacheDaoProvider;

  private final Provider<TitleSnapshotDao> titleSnapshotDaoProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<Clock> clockProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private ProfileRepository_Factory(Provider<ProfileDao> profileDaoProvider,
      Provider<RecommendationStateDao> recommendationStateDaoProvider,
      Provider<RecommendationCacheDao> recommendationCacheDaoProvider,
      Provider<TitleSnapshotDao> titleSnapshotDaoProvider,
      Provider<SettingsRepository> settingsProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    this.profileDaoProvider = profileDaoProvider;
    this.recommendationStateDaoProvider = recommendationStateDaoProvider;
    this.recommendationCacheDaoProvider = recommendationCacheDaoProvider;
    this.titleSnapshotDaoProvider = titleSnapshotDaoProvider;
    this.settingsProvider = settingsProvider;
    this.clockProvider = clockProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(profileDaoProvider.get(), recommendationStateDaoProvider.get(), recommendationCacheDaoProvider.get(), titleSnapshotDaoProvider.get(), settingsProvider.get(), clockProvider.get(), ioProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<ProfileDao> profileDaoProvider,
      Provider<RecommendationStateDao> recommendationStateDaoProvider,
      Provider<RecommendationCacheDao> recommendationCacheDaoProvider,
      Provider<TitleSnapshotDao> titleSnapshotDaoProvider,
      Provider<SettingsRepository> settingsProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    return new ProfileRepository_Factory(profileDaoProvider, recommendationStateDaoProvider, recommendationCacheDaoProvider, titleSnapshotDaoProvider, settingsProvider, clockProvider, ioProvider);
  }

  public static ProfileRepository newInstance(ProfileDao profileDao,
      RecommendationStateDao recommendationStateDao, RecommendationCacheDao recommendationCacheDao,
      TitleSnapshotDao titleSnapshotDao, SettingsRepository settings, Clock clock,
      CoroutineDispatcher io) {
    return new ProfileRepository(profileDao, recommendationStateDao, recommendationCacheDao, titleSnapshotDao, settings, clock, io);
  }
}
