package com.nexient.jazzy.feature.profiles;

import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.settings.SettingsRepository;
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
public final class ProfileEditViewModel_Factory {
  private final Provider<ProfileRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private ProfileEditViewModel_Factory(Provider<ProfileRepository> repositoryProvider,
      Provider<SettingsRepository> settingsProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsProvider = settingsProvider;
  }

  public ProfileEditViewModel get(Long profileId, boolean firstRun) {
    return newInstance(profileId, firstRun, repositoryProvider.get(), settingsProvider.get());
  }

  public static ProfileEditViewModel_Factory create(Provider<ProfileRepository> repositoryProvider,
      Provider<SettingsRepository> settingsProvider) {
    return new ProfileEditViewModel_Factory(repositoryProvider, settingsProvider);
  }

  public static ProfileEditViewModel newInstance(Long profileId, boolean firstRun,
      ProfileRepository repository, SettingsRepository settings) {
    return new ProfileEditViewModel(profileId, firstRun, repository, settings);
  }
}
