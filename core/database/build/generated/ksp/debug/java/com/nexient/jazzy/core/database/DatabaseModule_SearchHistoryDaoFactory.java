package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.app.AppDatabase;
import com.nexient.jazzy.core.database.app.SearchHistoryDao;
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
public final class DatabaseModule_SearchHistoryDaoFactory implements Factory<SearchHistoryDao> {
  private final Provider<AppDatabase> dbProvider;

  private DatabaseModule_SearchHistoryDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SearchHistoryDao get() {
    return searchHistoryDao(dbProvider.get());
  }

  public static DatabaseModule_SearchHistoryDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_SearchHistoryDaoFactory(dbProvider);
  }

  public static SearchHistoryDao searchHistoryDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.searchHistoryDao(db));
  }
}
