package com.nexient.jazzy.core.data.catalog;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.database.cache.TmdbCacheDao;
import com.nexient.jazzy.core.network.TmdbApi;
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
public final class CatalogRepository_Factory implements Factory<CatalogRepository> {
  private final Provider<TmdbApi> apiProvider;

  private final Provider<TmdbCacheDao> cacheProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<Clock> clockProvider;

  private final Provider<RegionProvider> regionProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private CatalogRepository_Factory(Provider<TmdbApi> apiProvider,
      Provider<TmdbCacheDao> cacheProvider, Provider<Json> jsonProvider,
      Provider<Clock> clockProvider, Provider<RegionProvider> regionProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    this.apiProvider = apiProvider;
    this.cacheProvider = cacheProvider;
    this.jsonProvider = jsonProvider;
    this.clockProvider = clockProvider;
    this.regionProvider = regionProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public CatalogRepository get() {
    return newInstance(apiProvider.get(), cacheProvider.get(), jsonProvider.get(), clockProvider.get(), regionProvider.get(), ioProvider.get());
  }

  public static CatalogRepository_Factory create(Provider<TmdbApi> apiProvider,
      Provider<TmdbCacheDao> cacheProvider, Provider<Json> jsonProvider,
      Provider<Clock> clockProvider, Provider<RegionProvider> regionProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    return new CatalogRepository_Factory(apiProvider, cacheProvider, jsonProvider, clockProvider, regionProvider, ioProvider);
  }

  public static CatalogRepository newInstance(TmdbApi api, TmdbCacheDao cache, Json json,
      Clock clock, RegionProvider region, CoroutineDispatcher io) {
    return new CatalogRepository(api, cache, json, clock, region, io);
  }
}
