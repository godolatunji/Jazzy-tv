package com.nexient.jazzy.core.data.catalog;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.database.app.SearchHistoryDao;
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
public final class SearchRepository_Factory implements Factory<SearchRepository> {
  private final Provider<TmdbApi> apiProvider;

  private final Provider<TmdbCacheDao> cacheProvider;

  private final Provider<SearchHistoryDao> historyProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<Clock> clockProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private SearchRepository_Factory(Provider<TmdbApi> apiProvider,
      Provider<TmdbCacheDao> cacheProvider, Provider<SearchHistoryDao> historyProvider,
      Provider<Json> jsonProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    this.apiProvider = apiProvider;
    this.cacheProvider = cacheProvider;
    this.historyProvider = historyProvider;
    this.jsonProvider = jsonProvider;
    this.clockProvider = clockProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public SearchRepository get() {
    return newInstance(apiProvider.get(), cacheProvider.get(), historyProvider.get(), jsonProvider.get(), clockProvider.get(), ioProvider.get());
  }

  public static SearchRepository_Factory create(Provider<TmdbApi> apiProvider,
      Provider<TmdbCacheDao> cacheProvider, Provider<SearchHistoryDao> historyProvider,
      Provider<Json> jsonProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider) {
    return new SearchRepository_Factory(apiProvider, cacheProvider, historyProvider, jsonProvider, clockProvider, ioProvider);
  }

  public static SearchRepository newInstance(TmdbApi api, TmdbCacheDao cache,
      SearchHistoryDao history, Json json, Clock clock, CoroutineDispatcher io) {
    return new SearchRepository(api, cache, history, json, clock, io);
  }
}
