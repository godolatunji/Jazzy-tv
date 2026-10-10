package com.nexient.jazzy.feature.home;

import com.nexient.jazzy.core.data.NetworkMonitor;
import com.nexient.jazzy.core.data.catalog.CatalogRepository;
import com.nexient.jazzy.core.data.catalog.TitleRepository;
import com.nexient.jazzy.core.data.library.LibraryRepository;
import com.nexient.jazzy.core.data.library.NextEpisodeResolver;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.recommendations.RecommendationRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<CatalogRepository> catalogProvider;

  private final Provider<TitleRepository> titlesProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<RecommendationRepository> recommendationsProvider;

  private final Provider<NextEpisodeResolver> nextEpisodesProvider;

  private final Provider<NetworkMonitor> networkProvider;

  private HomeViewModel_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<CatalogRepository> catalogProvider, Provider<TitleRepository> titlesProvider,
      Provider<LibraryRepository> libraryProvider,
      Provider<RecommendationRepository> recommendationsProvider,
      Provider<NextEpisodeResolver> nextEpisodesProvider,
      Provider<NetworkMonitor> networkProvider) {
    this.profilesProvider = profilesProvider;
    this.catalogProvider = catalogProvider;
    this.titlesProvider = titlesProvider;
    this.libraryProvider = libraryProvider;
    this.recommendationsProvider = recommendationsProvider;
    this.nextEpisodesProvider = nextEpisodesProvider;
    this.networkProvider = networkProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(profilesProvider.get(), catalogProvider.get(), titlesProvider.get(), libraryProvider.get(), recommendationsProvider.get(), nextEpisodesProvider.get(), networkProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<CatalogRepository> catalogProvider, Provider<TitleRepository> titlesProvider,
      Provider<LibraryRepository> libraryProvider,
      Provider<RecommendationRepository> recommendationsProvider,
      Provider<NextEpisodeResolver> nextEpisodesProvider,
      Provider<NetworkMonitor> networkProvider) {
    return new HomeViewModel_Factory(profilesProvider, catalogProvider, titlesProvider, libraryProvider, recommendationsProvider, nextEpisodesProvider, networkProvider);
  }

  public static HomeViewModel newInstance(ProfileRepository profiles, CatalogRepository catalog,
      TitleRepository titles, LibraryRepository library, RecommendationRepository recommendations,
      NextEpisodeResolver nextEpisodes, NetworkMonitor network) {
    return new HomeViewModel(profiles, catalog, titles, library, recommendations, nextEpisodes, network);
  }
}
