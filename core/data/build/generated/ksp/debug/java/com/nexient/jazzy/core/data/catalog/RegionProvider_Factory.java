package com.nexient.jazzy.core.data.catalog;

import com.nexient.jazzy.core.data.settings.SettingsRepository;
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
public final class RegionProvider_Factory implements Factory<RegionProvider> {
  private final Provider<SettingsRepository> settingsProvider;

  private RegionProvider_Factory(Provider<SettingsRepository> settingsProvider) {
    this.settingsProvider = settingsProvider;
  }

  @Override
  public RegionProvider get() {
    return newInstance(settingsProvider.get());
  }

  public static RegionProvider_Factory create(Provider<SettingsRepository> settingsProvider) {
    return new RegionProvider_Factory(settingsProvider);
  }

  public static RegionProvider newInstance(SettingsRepository settings) {
    return new RegionProvider(settings);
  }
}
