package com.nexient.jazzy.feature.settings;

import com.nexient.jazzy.core.data.catalog.SearchRepository;
import com.nexient.jazzy.core.data.library.LibraryRepository;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.recommendations.RecommendationRepository;
import com.nexient.jazzy.core.data.settings.SettingsRepository;
import com.nexient.jazzy.core.data.settings.TmdbCacheCleaner;
import com.nexient.jazzy.core.torrent.TorrentEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<SearchRepository> searchProvider;

  private final Provider<RecommendationRepository> recommendationsProvider;

  private final Provider<TorrentEngine> torrentEngineProvider;

  private final Provider<TmdbCacheCleaner> tmdbCacheProvider;

  private SettingsViewModel_Factory(Provider<SettingsRepository> settingsProvider,
      Provider<ProfileRepository> profilesProvider, Provider<LibraryRepository> libraryProvider,
      Provider<SearchRepository> searchProvider,
      Provider<RecommendationRepository> recommendationsProvider,
      Provider<TorrentEngine> torrentEngineProvider, Provider<TmdbCacheCleaner> tmdbCacheProvider) {
    this.settingsProvider = settingsProvider;
    this.profilesProvider = profilesProvider;
    this.libraryProvider = libraryProvider;
    this.searchProvider = searchProvider;
    this.recommendationsProvider = recommendationsProvider;
    this.torrentEngineProvider = torrentEngineProvider;
    this.tmdbCacheProvider = tmdbCacheProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsProvider.get(), profilesProvider.get(), libraryProvider.get(), searchProvider.get(), recommendationsProvider.get(), torrentEngineProvider.get(), tmdbCacheProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SettingsRepository> settingsProvider,
      Provider<ProfileRepository> profilesProvider, Provider<LibraryRepository> libraryProvider,
      Provider<SearchRepository> searchProvider,
      Provider<RecommendationRepository> recommendationsProvider,
      Provider<TorrentEngine> torrentEngineProvider, Provider<TmdbCacheCleaner> tmdbCacheProvider) {
    return new SettingsViewModel_Factory(settingsProvider, profilesProvider, libraryProvider, searchProvider, recommendationsProvider, torrentEngineProvider, tmdbCacheProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settings,
      ProfileRepository profiles, LibraryRepository library, SearchRepository search,
      RecommendationRepository recommendations, TorrentEngine torrentEngine,
      TmdbCacheCleaner tmdbCache) {
    return new SettingsViewModel(settings, profiles, library, search, recommendations, torrentEngine, tmdbCache);
  }
}
