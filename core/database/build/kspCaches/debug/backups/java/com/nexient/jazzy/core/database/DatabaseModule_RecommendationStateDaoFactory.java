package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.app.AppDatabase;
import com.nexient.jazzy.core.database.app.RecommendationStateDao;
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
public final class DatabaseModule_RecommendationStateDaoFactory implements Factory<RecommendationStateDao> {
  private final Provider<AppDatabase> dbProvider;

  private DatabaseModule_RecommendationStateDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public RecommendationStateDao get() {
    return recommendationStateDao(dbProvider.get());
  }

  public static DatabaseModule_RecommendationStateDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_RecommendationStateDaoFactory(dbProvider);
  }

  public static RecommendationStateDao recommendationStateDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.recommendationStateDao(db));
  }
}
