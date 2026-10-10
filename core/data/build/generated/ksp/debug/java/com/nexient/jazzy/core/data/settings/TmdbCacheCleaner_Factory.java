package com.nexient.jazzy.core.data.settings;

import android.content.Context;
import com.nexient.jazzy.core.database.cache.CacheDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.nexient.jazzy.core.common.IoDispatcher"
})
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
public final class TmdbCacheCleaner_Factory implements Factory<TmdbCacheCleaner> {
  private final Provider<CacheDatabase> cacheDatabaseProvider;

  private final Provider<Context> contextProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private TmdbCacheCleaner_Factory(Provider<CacheDatabase> cacheDatabaseProvider,
      Provider<Context> contextProvider, Provider<CoroutineDispatcher> ioProvider) {
    this.cacheDatabaseProvider = cacheDatabaseProvider;
    this.contextProvider = contextProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public TmdbCacheCleaner get() {
    return newInstance(cacheDatabaseProvider.get(), contextProvider.get(), ioProvider.get());
  }

  public static TmdbCacheCleaner_Factory create(Provider<CacheDatabase> cacheDatabaseProvider,
      Provider<Context> contextProvider, Provider<CoroutineDispatcher> ioProvider) {
    return new TmdbCacheCleaner_Factory(cacheDatabaseProvider, contextProvider, ioProvider);
  }

  public static TmdbCacheCleaner newInstance(CacheDatabase cacheDatabase, Context context,
      CoroutineDispatcher io) {
    return new TmdbCacheCleaner(cacheDatabase, context, io);
  }
}
