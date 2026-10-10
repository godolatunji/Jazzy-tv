package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.cache.CacheDatabase;
import com.nexient.jazzy.core.database.cache.SourceCacheDao;
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
public final class DatabaseModule_SourceCacheDaoFactory implements Factory<SourceCacheDao> {
  private final Provider<CacheDatabase> dbProvider;

  private DatabaseModule_SourceCacheDaoFactory(Provider<CacheDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SourceCacheDao get() {
    return sourceCacheDao(dbProvider.get());
  }

  public static DatabaseModule_SourceCacheDaoFactory create(Provider<CacheDatabase> dbProvider) {
    return new DatabaseModule_SourceCacheDaoFactory(dbProvider);
  }

  public static SourceCacheDao sourceCacheDao(CacheDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.sourceCacheDao(db));
  }
}
