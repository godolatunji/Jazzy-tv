package com.nexient.jazzy.feature.settings;

import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.sources.DbProviderSettingsStore;
import com.nexient.jazzy.core.data.sources.SourceRepository;
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
public final class SitesSettingsViewModel_Factory implements Factory<SitesSettingsViewModel> {
  private final Provider<SourceRepository> sourcesProvider;

  private final Provider<DbProviderSettingsStore> storeProvider;

  private final Provider<ProfileRepository> profilesProvider;

  private SitesSettingsViewModel_Factory(Provider<SourceRepository> sourcesProvider,
      Provider<DbProviderSettingsStore> storeProvider,
      Provider<ProfileRepository> profilesProvider) {
    this.sourcesProvider = sourcesProvider;
    this.storeProvider = storeProvider;
    this.profilesProvider = profilesProvider;
  }

  @Override
  public SitesSettingsViewModel get() {
    return newInstance(sourcesProvider.get(), storeProvider.get(), profilesProvider.get());
  }

  public static SitesSettingsViewModel_Factory create(Provider<SourceRepository> sourcesProvider,
      Provider<DbProviderSettingsStore> storeProvider,
      Provider<ProfileRepository> profilesProvider) {
    return new SitesSettingsViewModel_Factory(sourcesProvider, storeProvider, profilesProvider);
  }

  public static SitesSettingsViewModel newInstance(SourceRepository sources,
      DbProviderSettingsStore store, ProfileRepository profiles) {
    return new SitesSettingsViewModel(sources, store, profiles);
  }
}
