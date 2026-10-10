package com.nexient.jazzy.core.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nexient.jazzy.core.network.TmdbHttpClient")
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
public final class NetworkModule_TmdbClientFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return tmdbClient();
  }

  public static NetworkModule_TmdbClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient tmdbClient() {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.tmdbClient());
  }

  private static final class InstanceHolder {
    static final NetworkModule_TmdbClientFactory INSTANCE = new NetworkModule_TmdbClientFactory();
  }
}
