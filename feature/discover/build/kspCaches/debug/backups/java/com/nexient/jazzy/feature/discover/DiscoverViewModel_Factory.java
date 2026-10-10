package com.nexient.jazzy.feature.discover;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.data.NetworkMonitor;
import com.nexient.jazzy.core.data.catalog.CatalogRepository;
import com.nexient.jazzy.core.data.catalog.TitleRepository;
import com.nexient.jazzy.core.data.library.LibraryRepository;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
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
public final class DiscoverViewModel_Factory implements Factory<DiscoverViewModel> {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<CatalogRepository> catalogProvider;

  private final Provider<TitleRepository> titlesProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<NetworkMonitor> networkProvider;

  private final Provider<Clock> clockProvider;

  private DiscoverViewModel_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<CatalogRepository> catalogProvider, Provider<TitleRepository> titlesProvider,
      Provider<LibraryRepository> libraryProvider, Provider<NetworkMonitor> networkProvider,
      Provider<Clock> clockProvider) {
    this.profilesProvider = profilesProvider;
    this.catalogProvider = catalogProvider;
    this.titlesProvider = titlesProvider;
    this.libraryProvider = libraryProvider;
    this.networkProvider = networkProvider;
    this.clockProvider = clockProvider;
  }

  @Override
  public DiscoverViewModel get() {
    return newInstance(profilesProvider.get(), catalogProvider.get(), titlesProvider.get(), libraryProvider.get(), networkProvider.get(), clockProvider.get());
  }

  public static DiscoverViewModel_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<CatalogRepository> catalogProvider, Provider<TitleRepository> titlesProvider,
      Provider<LibraryRepository> libraryProvider, Provider<NetworkMonitor> networkProvider,
      Provider<Clock> clockProvider) {
    return new DiscoverViewModel_Factory(profilesProvider, catalogProvider, titlesProvider, libraryProvider, networkProvider, clockProvider);
  }

  public static DiscoverViewModel newInstance(ProfileRepository profiles, CatalogRepository catalog,
      TitleRepository titles, LibraryRepository library, NetworkMonitor network, Clock clock) {
    return new DiscoverViewModel(profiles, catalog, titles, library, network, clock);
  }
}
