package com.nexient.jazzy.core.database;

import com.nexient.jazzy.core.database.app.AppDatabase;
import com.nexient.jazzy.core.database.app.ProviderDao;
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
public final class DatabaseModule_ProviderDaoFactory implements Factory<ProviderDao> {
  private final Provider<AppDatabase> dbProvider;

  private DatabaseModule_ProviderDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ProviderDao get() {
    return providerDao(dbProvider.get());
  }

  public static DatabaseModule_ProviderDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProviderDaoFactory(dbProvider);
  }

  public static ProviderDao providerDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providerDao(db));
  }
}
