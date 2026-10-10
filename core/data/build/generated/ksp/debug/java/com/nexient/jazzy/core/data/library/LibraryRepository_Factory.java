package com.nexient.jazzy.core.data.library;

import com.nexient.jazzy.core.common.Clock;
import com.nexient.jazzy.core.database.app.LibraryDao;
import com.nexient.jazzy.core.database.app.PlaySessionDao;
import com.nexient.jazzy.core.database.app.ProgressDao;
import com.nexient.jazzy.core.database.app.TitleSnapshotDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "com.nexient.jazzy.core.common.IoDispatcher",
    "com.nexient.jazzy.core.common.ApplicationScope"
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
public final class LibraryRepository_Factory implements Factory<LibraryRepository> {
  private final Provider<LibraryDao> libraryDaoProvider;

  private final Provider<ProgressDao> progressDaoProvider;

  private final Provider<PlaySessionDao> sessionDaoProvider;

  private final Provider<TitleSnapshotDao> snapshotDaoProvider;

  private final Provider<Clock> clockProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private final Provider<CoroutineScope> appScopeProvider;

  private LibraryRepository_Factory(Provider<LibraryDao> libraryDaoProvider,
      Provider<ProgressDao> progressDaoProvider, Provider<PlaySessionDao> sessionDaoProvider,
      Provider<TitleSnapshotDao> snapshotDaoProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider, Provider<CoroutineScope> appScopeProvider) {
    this.libraryDaoProvider = libraryDaoProvider;
    this.progressDaoProvider = progressDaoProvider;
    this.sessionDaoProvider = sessionDaoProvider;
    this.snapshotDaoProvider = snapshotDaoProvider;
    this.clockProvider = clockProvider;
    this.ioProvider = ioProvider;
    this.appScopeProvider = appScopeProvider;
  }

  @Override
  public LibraryRepository get() {
    return newInstance(libraryDaoProvider.get(), progressDaoProvider.get(), sessionDaoProvider.get(), snapshotDaoProvider.get(), clockProvider.get(), ioProvider.get(), appScopeProvider.get());
  }

  public static LibraryRepository_Factory create(Provider<LibraryDao> libraryDaoProvider,
      Provider<ProgressDao> progressDaoProvider, Provider<PlaySessionDao> sessionDaoProvider,
      Provider<TitleSnapshotDao> snapshotDaoProvider, Provider<Clock> clockProvider,
      Provider<CoroutineDispatcher> ioProvider, Provider<CoroutineScope> appScopeProvider) {
    return new LibraryRepository_Factory(libraryDaoProvider, progressDaoProvider, sessionDaoProvider, snapshotDaoProvider, clockProvider, ioProvider, appScopeProvider);
  }

  public static LibraryRepository newInstance(LibraryDao libraryDao, ProgressDao progressDao,
      PlaySessionDao sessionDao, TitleSnapshotDao snapshotDao, Clock clock, CoroutineDispatcher io,
      CoroutineScope appScope) {
    return new LibraryRepository(libraryDao, progressDao, sessionDao, snapshotDao, clock, io, appScope);
  }
}
