package com.nexient.jazzy.core.player;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class PlaybackService_MembersInjector implements MembersInjector<PlaybackService> {
  private final Provider<PlayerHolder> holderProvider;

  private PlaybackService_MembersInjector(Provider<PlayerHolder> holderProvider) {
    this.holderProvider = holderProvider;
  }

  @Override
  public void injectMembers(PlaybackService instance) {
    injectHolder(instance, holderProvider.get());
  }

  public static MembersInjector<PlaybackService> create(Provider<PlayerHolder> holderProvider) {
    return new PlaybackService_MembersInjector(holderProvider);
  }

  @InjectedFieldSignature("com.nexient.jazzy.core.player.PlaybackService.holder")
  public static void injectHolder(PlaybackService instance, PlayerHolder holder) {
    instance.holder = holder;
  }
}
