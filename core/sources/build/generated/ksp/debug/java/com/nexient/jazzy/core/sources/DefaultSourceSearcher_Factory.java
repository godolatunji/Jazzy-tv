package com.nexient.jazzy.core.sources;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class DefaultSourceSearcher_Factory implements Factory<DefaultSourceSearcher> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<ProviderSettingsStore> settingsStoreProvider;

  private DefaultSourceSearcher_Factory(Provider<OkHttpClient> clientProvider,
      Provider<ProviderSettingsStore> settingsStoreProvider) {
    this.clientProvider = clientProvider;
    this.settingsStoreProvider = settingsStoreProvider;
  }

  @Override
  public DefaultSourceSearcher get() {
    return newInstance(clientProvider.get(), settingsStoreProvider.get());
  }

  public static DefaultSourceSearcher_Factory create(Provider<OkHttpClient> clientProvider,
      Provider<ProviderSettingsStore> settingsStoreProvider) {
    return new DefaultSourceSearcher_Factory(clientProvider, settingsStoreProvider);
  }

  public static DefaultSourceSearcher newInstance(OkHttpClient client,
      ProviderSettingsStore settingsStore) {
    return new DefaultSourceSearcher(client, settingsStore);
  }
}
