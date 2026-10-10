package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.cache.CacheDatabase;
import com.nexient.jazzy.core.database.cache.RecommendationCacheDao;
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
public final class DatabaseModule_RecommendationCacheDaoFactory implements Factory<RecommendationCacheDao> {
  private final Provider<CacheDatabase> dbProvider;

  private DatabaseModule_RecommendationCacheDaoFactory(Provider<CacheDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public RecommendationCacheDao get() {
    return recommendationCacheDao(dbProvider.get());
  }

  public static DatabaseModule_RecommendationCacheDaoFactory create(
      Provider<CacheDatabase> dbProvider) {
    return new DatabaseModule_RecommendationCacheDaoFactory(dbProvider);
  }

  public static RecommendationCacheDao recommendationCacheDao(CacheDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.recommendationCacheDao(db));
  }
}
