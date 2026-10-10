package com.nexient.jazzy.feature.player;

import com.nexient.jazzy.core.model.PlaybackRequest;
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
public final class PlayerViewModel_Factory_Impl implements PlayerViewModel.Factory {
  private final PlayerViewModel_Factory delegateFactory;

  PlayerViewModel_Factory_Impl(PlayerViewModel_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public PlayerViewModel create(PlaybackRequest request) {
    return delegateFactory.get(request);
  }

  public static Provider<PlayerViewModel.Factory> create(PlayerViewModel_Factory delegateFactory) {
    return InstanceFactory.create(new PlayerViewModel_Factory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<PlayerViewModel.Factory> createFactoryProvider(
      PlayerViewModel_Factory delegateFactory) {
    return InstanceFactory.create(new PlayerViewModel_Factory_Impl(delegateFactory));
  }
}
