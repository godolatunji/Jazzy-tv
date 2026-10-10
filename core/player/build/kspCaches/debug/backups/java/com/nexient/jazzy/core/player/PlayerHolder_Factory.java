package com.nexient.jazzy.core.player;

import android.content.Context;
import com.nexient.jazzy.core.torrent.TorrentFileProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class PlayerHolder_Factory implements Factory<PlayerHolder> {
  private final Provider<Context> contextProvider;

  private final Provider<TorrentFileProvider> filesProvider;

  private PlayerHolder_Factory(Provider<Context> contextProvider,
      Provider<TorrentFileProvider> filesProvider) {
    this.contextProvider = contextProvider;
    this.filesProvider = filesProvider;
  }

  @Override
  public PlayerHolder get() {
    return newInstance(contextProvider.get(), filesProvider.get());
  }

  public static PlayerHolder_Factory create(Provider<Context> contextProvider,
      Provider<TorrentFileProvider> filesProvider) {
    return new PlayerHolder_Factory(contextProvider, filesProvider);
  }

  public static PlayerHolder newInstance(Context context, TorrentFileProvider files) {
    return new PlayerHolder(context, files);
  }
}
