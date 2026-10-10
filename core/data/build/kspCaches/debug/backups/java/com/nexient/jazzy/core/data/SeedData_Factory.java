package com.nexient.jazzy.core.data;

import com.nexient.jazzy.core.data.settings.SettingsRepository;
import com.nexient.jazzy.core.database.app.ProviderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SeedData_Factory implements Factory<SeedData> {
  private final Provider<ProviderDao> providerDaoProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private SeedData_Factory(Provider<ProviderDao> providerDaoProvider,
      Provider<SettingsRepository> settingsProvider) {
    this.providerDaoProvider = providerDaoProvider;
    this.settingsProvider = settingsProvider;
  }

  @Override
  public SeedData get() {
    return newInstance(providerDaoProvider.get(), settingsProvider.get());
  }

  public static SeedData_Factory create(Provider<ProviderDao> providerDaoProvider,
      Provider<SettingsRepository> settingsProvider) {
    return new SeedData_Factory(providerDaoProvider, settingsProvider);
  }

  public static SeedData newInstance(ProviderDao providerDao, SettingsRepository settings) {
    return new SeedData(providerDao, settings);
  }
}
