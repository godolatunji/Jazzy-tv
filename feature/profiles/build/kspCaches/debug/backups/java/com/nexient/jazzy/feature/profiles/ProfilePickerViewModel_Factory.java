package com.nexient.jazzy.feature.profiles;

import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.settings.SettingsRepository;
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
public final class ProfilePickerViewModel_Factory implements Factory<ProfilePickerViewModel> {
  private final Provider<ProfileRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private ProfilePickerViewModel_Factory(Provider<ProfileRepository> repositoryProvider,
      Provider<SettingsRepository> settingsProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsProvider = settingsProvider;
  }

  @Override
  public ProfilePickerViewModel get() {
    return newInstance(repositoryProvider.get(), settingsProvider.get());
  }

  public static ProfilePickerViewModel_Factory create(
      Provider<ProfileRepository> repositoryProvider,
      Provider<SettingsRepository> settingsProvider) {
    return new ProfilePickerViewModel_Factory(repositoryProvider, settingsProvider);
  }

  public static ProfilePickerViewModel newInstance(ProfileRepository repository,
      SettingsRepository settings) {
    return new ProfilePickerViewModel(repository, settings);
  }
}
