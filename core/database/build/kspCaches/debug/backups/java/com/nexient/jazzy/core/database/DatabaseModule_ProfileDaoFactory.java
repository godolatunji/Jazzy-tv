package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.app.AppDatabase;
import com.nexient.jazzy.core.database.app.ProfileDao;
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
public final class DatabaseModule_ProfileDaoFactory implements Factory<ProfileDao> {
  private final Provider<AppDatabase> dbProvider;

  private DatabaseModule_ProfileDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ProfileDao get() {
    return profileDao(dbProvider.get());
  }

  public static DatabaseModule_ProfileDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProfileDaoFactory(dbProvider);
  }

  public static ProfileDao profileDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.profileDao(db));
  }
}
