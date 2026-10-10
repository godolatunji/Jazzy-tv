package com.nexient.jazzy.core.data.sources;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.database.app.ProviderDao;
import com.nexient.jazzy.core.database.cache.SourceCacheDao;
import com.nexient.jazzy.core.sources.SourceSearcher;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.serialization.json.Json;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nexient.jazzy.core.common.IoDispatcher")
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
public final class SourceRepository_Factory implements Factory<SourceRepository> {
  private final Provider<SourceSearcher> searcherProvider;

  private final Provider<SourceCacheDao> cacheDaoProvider;

  private final Provider<ProviderDao> providerDaoProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<Clock> clockProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private SourceRepository_Factory(Provider<SourceSearcher> searcherProvider,
      Provider<SourceCacheDao> cacheDaoProvider, Provider<ProviderDao> providerDaoProvider,
      Provider<Json> jsonProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    this.searcherProvider = searcherProvider;
    this.cacheDaoProvider = cacheDaoProvider;
    this.providerDaoProvider = providerDaoProvider;
    this.jsonProvider = jsonProvider;
    this.clockProvider = clockProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public SourceRepository get() {
    return newInstance(searcherProvider.get(), cacheDaoProvider.get(), providerDaoProvider.get(), jsonProvider.get(), clockProvider.get(), ioProvider.get());
  }

  public static SourceRepository_Factory create(Provider<SourceSearcher> searcherProvider,
      Provider<SourceCacheDao> cacheDaoProvider, Provider<ProviderDao> providerDaoProvider,
      Provider<Json> jsonProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    return new SourceRepository_Factory(searcherProvider, cacheDaoProvider, providerDaoProvider, jsonProvider, clockProvider, ioProvider);
  }

  public static SourceRepository newInstance(SourceSearcher searcher, SourceCacheDao cacheDao,
      ProviderDao providerDao, Json json, Clock clock, CoroutineDispatcher io) {
    return new SourceRepository(searcher, cacheDao, providerDao, json, clock, io);
  }
}
