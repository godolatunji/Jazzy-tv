package com.nexient.jazzy.core.data.settings;

import com.nexient.jazzy.core.database.app.SettingsDao;
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
public final class SettingsRepository_Factory implements Factory<SettingsRepository> {
  private final Provider<SettingsDao> daoProvider;

  private SettingsRepository_Factory(Provider<SettingsDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public SettingsRepository get() {
    return newInstance(daoProvider.get());
  }

  public static SettingsRepository_Factory create(Provider<SettingsDao> daoProvider) {
    return new SettingsRepository_Factory(daoProvider);
  }

  public static SettingsRepository newInstance(SettingsDao dao) {
    return new SettingsRepository(dao);
  }
}
