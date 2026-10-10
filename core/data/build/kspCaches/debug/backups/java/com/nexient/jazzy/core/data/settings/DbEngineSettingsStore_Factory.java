package com.nexient.jazzy.core.data.settings;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DbEngineSettingsStore_Factory implements Factory<DbEngineSettingsStore> {
  private final Provider<SettingsRepository> repositoryProvider;

  private DbEngineSettingsStore_Factory(Provider<SettingsRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public DbEngineSettingsStore get() {
    return newInstance(repositoryProvider.get());
  }

  public static DbEngineSettingsStore_Factory create(
      Provider<SettingsRepository> repositoryProvider) {
    return new DbEngineSettingsStore_Factory(repositoryProvider);
  }

  public static DbEngineSettingsStore newInstance(SettingsRepository repository) {
    return new DbEngineSettingsStore(repository);
  }
}
