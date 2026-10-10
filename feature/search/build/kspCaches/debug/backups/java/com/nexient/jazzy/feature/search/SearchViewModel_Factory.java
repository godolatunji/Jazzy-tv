package com.nexient.jazzy.feature.search;

import com.nexient.jazzy.core.data.NetworkMonitor;
import com.nexient.jazzy.core.data.catalog.CatalogRepository;
import com.nexient.jazzy.core.data.catalog.SearchRepository;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<SearchRepository> searchProvider;

  private final Provider<CatalogRepository> catalogProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<NetworkMonitor> networkProvider;

  private SearchViewModel_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<SearchRepository> searchProvider, Provider<CatalogRepository> catalogProvider,
      Provider<LibraryRepository> libraryProvider, Provider<NetworkMonitor> networkProvider) {
    this.profilesProvider = profilesProvider;
    this.searchProvider = searchProvider;
    this.catalogProvider = catalogProvider;
    this.libraryProvider = libraryProvider;
    this.networkProvider = networkProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(profilesProvider.get(), searchProvider.get(), catalogProvider.get(), libraryProvider.get(), networkProvider.get());
  }

  public static SearchViewModel_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<SearchRepository> searchProvider, Provider<CatalogRepository> catalogProvider,
      Provider<LibraryRepository> libraryProvider, Provider<NetworkMonitor> networkProvider) {
    return new SearchViewModel_Factory(profilesProvider, searchProvider, catalogProvider, libraryProvider, networkProvider);
  }

  public static SearchViewModel newInstance(ProfileRepository profiles, SearchRepository search,
      CatalogRepository catalog, LibraryRepository library, NetworkMonitor network) {
    return new SearchViewModel(profiles, search, catalog, library, network);
  }
}
