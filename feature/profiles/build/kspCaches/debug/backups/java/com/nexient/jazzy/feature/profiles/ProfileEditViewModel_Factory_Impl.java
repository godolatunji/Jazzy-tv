package com.nexient.jazzy.feature.profiles;

import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ProfileEditViewModel_Factory_Impl implements ProfileEditViewModel.Factory {
  private final ProfileEditViewModel_Factory delegateFactory;

  ProfileEditViewModel_Factory_Impl(ProfileEditViewModel_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public ProfileEditViewModel create(Long profileId, boolean firstRun) {
    return delegateFactory.get(profileId, firstRun);
  }

  public static Provider<ProfileEditViewModel.Factory> create(
      ProfileEditViewModel_Factory delegateFactory) {
    return InstanceFactory.create(new ProfileEditViewModel_Factory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<ProfileEditViewModel.Factory> createFactoryProvider(
      ProfileEditViewModel_Factory delegateFactory) {
    return InstanceFactory.create(new ProfileEditViewModel_Factory_Impl(delegateFactory));
  }
}
