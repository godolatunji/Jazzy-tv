package com.nexient.jazzy.core.data.recommendations;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.data.catalog.CatalogRepository;
import com.nexient.jazzy.core.database.app.LibraryDao;
import com.nexient.jazzy.core.database.app.PlaySessionDao;
import com.nexient.jazzy.core.database.app.ProgressDao;
import com.nexient.jazzy.core.database.app.RecommendationStateDao;
import com.nexient.jazzy.core.database.app.TitleSnapshotDao;
import com.nexient.jazzy.core.database.cache.RecommendationCacheDao;
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
public final class RecommendationRepository_Factory implements Factory<RecommendationRepository> {
  private final Provider<ProgressDao> progressDaoProvider;

  private final Provider<PlaySessionDao> sessionDaoProvider;

  private final Provider<LibraryDao> libraryDaoProvider;

  private final Provider<TitleSnapshotDao> snapshotDaoProvider;

  private final Provider<RecommendationStateDao> stateDaoProvider;

  private final Provider<RecommendationCacheDao> cacheDaoProvider;

  private final Provider<CatalogRepository> catalogProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<Clock> clockProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private RecommendationRepository_Factory(Provider<ProgressDao> progressDaoProvider,
      Provider<PlaySessionDao> sessionDaoProvider, Provider<LibraryDao> libraryDaoProvider,
      Provider<TitleSnapshotDao> snapshotDaoProvider,
      Provider<RecommendationStateDao> stateDaoProvider,
      Provider<RecommendationCacheDao> cacheDaoProvider,
      Provider<CatalogRepository> catalogProvider, Provider<Json> jsonProvider,
      Provider<Clock> clockProvider, Provider<CoroutineDispatcher> ioProvider) {
    this.progressDaoProvider = progressDaoProvider;
    this.sessionDaoProvider = sessionDaoProvider;
    this.libraryDaoProvider = libraryDaoProvider;
    this.snapshotDaoProvider = snapshotDaoProvider;
    this.stateDaoProvider = stateDaoProvider;
    this.cacheDaoProvider = cacheDaoProvider;
    this.catalogProvider = catalogProvider;
    this.jsonProvider = jsonProvider;
    this.clockProvider = clockProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public RecommendationRepository get() {
    return newInstance(progressDaoProvider.get(), sessionDaoProvider.get(), libraryDaoProvider.get(), snapshotDaoProvider.get(), stateDaoProvider.get(), cacheDaoProvider.get(), catalogProvider.get(), jsonProvider.get(), clockProvider.get(), ioProvider.get());
  }

  public static RecommendationRepository_Factory create(Provider<ProgressDao> progressDaoProvider,
      Provider<PlaySessionDao> sessionDaoProvider, Provider<LibraryDao> libraryDaoProvider,
      Provider<TitleSnapshotDao> snapshotDaoProvider,
      Provider<RecommendationStateDao> stateDaoProvider,
      Provider<RecommendationCacheDao> cacheDaoProvider,
      Provider<CatalogRepository> catalogProvider, Provider<Json> jsonProvider,
      Provider<Clock> clockProvider, Provider<CoroutineDispatcher> ioProvider) {
    return new RecommendationRepository_Factory(progressDaoProvider, sessionDaoProvider, libraryDaoProvider, snapshotDaoProvider, stateDaoProvider, cacheDaoProvider, catalogProvider, jsonProvider, clockProvider, ioProvider);
  }

  public static RecommendationRepository newInstance(ProgressDao progressDao,
      PlaySessionDao sessionDao, LibraryDao libraryDao, TitleSnapshotDao snapshotDao,
      RecommendationStateDao stateDao, RecommendationCacheDao cacheDao, CatalogRepository catalog,
      Json json, Clock clock, CoroutineDispatcher io) {
    return new RecommendationRepository(progressDao, sessionDao, libraryDao, snapshotDao, stateDao, cacheDao, catalog, json, clock, io);
  }
}
