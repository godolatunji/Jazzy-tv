package com.nexient.jazzy.core.data.sources;

import com.nexient.jazzy.core.database.app.ProviderDao;
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
public final class DbTrackerListStore_Factory implements Factory<DbTrackerListStore> {
  private final Provider<ProviderDao> daoProvider;

  private DbTrackerListStore_Factory(Provider<ProviderDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public DbTrackerListStore get() {
    return newInstance(daoProvider.get());
  }

  public static DbTrackerListStore_Factory create(Provider<ProviderDao> daoProvider) {
    return new DbTrackerListStore_Factory(daoProvider);
  }

  public static DbTrackerListStore newInstance(ProviderDao dao) {
    return new DbTrackerListStore(dao);
  }
}
