package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.app.AppDatabase;
import com.nexient.jazzy.core.database.app.PlaySessionDao;
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
public final class DatabaseModule_PlaySessionDaoFactory implements Factory<PlaySessionDao> {
  private final Provider<AppDatabase> dbProvider;

  private DatabaseModule_PlaySessionDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PlaySessionDao get() {
    return playSessionDao(dbProvider.get());
  }

  public static DatabaseModule_PlaySessionDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_PlaySessionDaoFactory(dbProvider);
  }

  public static PlaySessionDao playSessionDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.playSessionDao(db));
  }
}
