package com.nexient.jazzy.core.data.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.database.app.TitleSnapshotDao;
import com.nexient.jazzy.core.database.cache.SourceCacheDao;
import com.nexient.jazzy.core.database.cache.TmdbCacheDao;
import dagger.internal.DaggerGenerated;
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
public final class CacheEvictionWorker_Factory {
  private final Provider<TmdbCacheDao> tmdbProvider;

  private final Provider<SourceCacheDao> sourcesProvider;

  private final Provider<TitleSnapshotDao> snapshotsProvider;

  private final Provider<Clock> clockProvider;

  private CacheEvictionWorker_Factory(Provider<TmdbCacheDao> tmdbProvider,
      Provider<SourceCacheDao> sourcesProvider, Provider<TitleSnapshotDao> snapshotsProvider,
      Provider<Clock> clockProvider) {
    this.tmdbProvider = tmdbProvider;
    this.sourcesProvider = sourcesProvider;
    this.snapshotsProvider = snapshotsProvider;
    this.clockProvider = clockProvider;
  }

  public CacheEvictionWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, tmdbProvider.get(), sourcesProvider.get(), snapshotsProvider.get(), clockProvider.get());
  }

  public static CacheEvictionWorker_Factory create(Provider<TmdbCacheDao> tmdbProvider,
      Provider<SourceCacheDao> sourcesProvider, Provider<TitleSnapshotDao> snapshotsProvider,
      Provider<Clock> clockProvider) {
    return new CacheEvictionWorker_Factory(tmdbProvider, sourcesProvider, snapshotsProvider, clockProvider);
  }

  public static CacheEvictionWorker newInstance(Context context, WorkerParameters params,
      TmdbCacheDao tmdb, SourceCacheDao sources, TitleSnapshotDao snapshots, Clock clock) {
    return new CacheEvictionWorker(context, params, tmdb, sources, snapshots, clock);
  }
}
