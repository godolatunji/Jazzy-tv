package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.cache.CacheDatabase;
import com.nexient.jazzy.core.database.cache.TmdbCacheDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class DatabaseModule_TmdbCacheDaoFactory implements Factory<TmdbCacheDao> {
  private final Provider<CacheDatabase> dbProvider;

  private DatabaseModule_TmdbCacheDaoFactory(Provider<CacheDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TmdbCacheDao get() {
    return tmdbCacheDao(dbProvider.get());
  }

  public static DatabaseModule_TmdbCacheDaoFactory create(Provider<CacheDatabase> dbProvider) {
    return new DatabaseModule_TmdbCacheDaoFactory(dbProvider);
  }

  public static TmdbCacheDao tmdbCacheDao(CacheDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.tmdbCacheDao(db));
  }
}
