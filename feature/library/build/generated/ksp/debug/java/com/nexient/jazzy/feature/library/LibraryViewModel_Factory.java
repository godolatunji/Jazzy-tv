package com.nexient.jazzy.feature.library;

import com.nexient.jazzy.core.common.Clock;
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<Clock> clockProvider;

  private LibraryViewModel_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<LibraryRepository> libraryProvider, Provider<Clock> clockProvider) {
    this.profilesProvider = profilesProvider;
    this.libraryProvider = libraryProvider;
    this.clockProvider = clockProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(profilesProvider.get(), libraryProvider.get(), clockProvider.get());
  }

  public static LibraryViewModel_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<LibraryRepository> libraryProvider, Provider<Clock> clockProvider) {
    return new LibraryViewModel_Factory(profilesProvider, libraryProvider, clockProvider);
  }

  public static LibraryViewModel newInstance(ProfileRepository profiles, LibraryRepository library,
      Clock clock) {
    return new LibraryViewModel(profiles, library, clock);
  }
}
