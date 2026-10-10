package com.nexient.jazzy.feature.details;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.data.catalog.TitleRepository;
import com.nexient.jazzy.core.data.device.DeviceCapabilities;
import com.nexient.jazzy.core.data.library.LibraryRepository;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.settings.SettingsRepository;
import com.nexient.jazzy.core.data.sources.SourceRepository;
import dagger.internal.DaggerGenerated;
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
public final class DetailsViewModel_Factory {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<TitleRepository> titlesProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<SourceRepository> sourcesRepoProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<DeviceCapabilities> capabilitiesProvider;

  private final Provider<Clock> clockProvider;

  private DetailsViewModel_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<TitleRepository> titlesProvider, Provider<LibraryRepository> libraryProvider,
      Provider<SourceRepository> sourcesRepoProvider, Provider<SettingsRepository> settingsProvider,
      Provider<DeviceCapabilities> capabilitiesProvider, Provider<Clock> clockProvider) {
    this.profilesProvider = profilesProvider;
    this.titlesProvider = titlesProvider;
    this.libraryProvider = libraryProvider;
    this.sourcesRepoProvider = sourcesRepoProvider;
    this.settingsProvider = settingsProvider;
    this.capabilitiesProvider = capabilitiesProvider;
    this.clockProvider = clockProvider;
  }

  public DetailsViewModel get(DetailsArgs args) {
    return newInstance(args, profilesProvider.get(), titlesProvider.get(), libraryProvider.get(), sourcesRepoProvider.get(), settingsProvider.get(), capabilitiesProvider.get(), clockProvider.get());
  }

  public static DetailsViewModel_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<TitleRepository> titlesProvider, Provider<LibraryRepository> libraryProvider,
      Provider<SourceRepository> sourcesRepoProvider, Provider<SettingsRepository> settingsProvider,
      Provider<DeviceCapabilities> capabilitiesProvider, Provider<Clock> clockProvider) {
    return new DetailsViewModel_Factory(profilesProvider, titlesProvider, libraryProvider, sourcesRepoProvider, settingsProvider, capabilitiesProvider, clockProvider);
  }

  public static DetailsViewModel newInstance(DetailsArgs args, ProfileRepository profiles,
      TitleRepository titles, LibraryRepository library, SourceRepository sourcesRepo,
      SettingsRepository settings, DeviceCapabilities capabilities, Clock clock) {
    return new DetailsViewModel(args, profiles, titles, library, sourcesRepo, settings, capabilities, clock);
  }
}
