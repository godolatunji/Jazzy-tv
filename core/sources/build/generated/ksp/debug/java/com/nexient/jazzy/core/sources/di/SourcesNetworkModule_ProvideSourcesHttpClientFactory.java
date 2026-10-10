package com.nexient.jazzy.core.sources.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nexient.jazzy.core.sources.di.SourcesHttpClient")
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
public final class SourcesNetworkModule_ProvideSourcesHttpClientFactory implements Factory<OkHttpClient> {
  @Override
  public OkHttpClient get() {
    return provideSourcesHttpClient();
  }

  public static SourcesNetworkModule_ProvideSourcesHttpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OkHttpClient provideSourcesHttpClient() {
    return Preconditions.checkNotNullFromProvides(SourcesNetworkModule.INSTANCE.provideSourcesHttpClient());
  }

  private static final class InstanceHolder {
    static final SourcesNetworkModule_ProvideSourcesHttpClientFactory INSTANCE = new SourcesNetworkModule_ProvideSourcesHttpClientFactory();
  }
}
