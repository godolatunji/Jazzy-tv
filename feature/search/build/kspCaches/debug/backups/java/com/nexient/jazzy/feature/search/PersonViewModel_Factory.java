package com.nexient.jazzy.feature.search;

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
public final class PersonViewModel_Factory implements Factory<PersonViewModel> {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<SearchRepository> searchProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private PersonViewModel_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<SearchRepository> searchProvider, Provider<LibraryRepository> libraryProvider) {
    this.profilesProvider = profilesProvider;
    this.searchProvider = searchProvider;
    this.libraryProvider = libraryProvider;
  }

  @Override
  public PersonViewModel get() {
    return newInstance(profilesProvider.get(), searchProvider.get(), libraryProvider.get());
  }

  public static PersonViewModel_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<SearchRepository> searchProvider, Provider<LibraryRepository> libraryProvider) {
    return new PersonViewModel_Factory(profilesProvider, searchProvider, libraryProvider);
  }

  public static PersonViewModel newInstance(ProfileRepository profiles, SearchRepository search,
      LibraryRepository library) {
    return new PersonViewModel(profiles, search, library);
  }
}
