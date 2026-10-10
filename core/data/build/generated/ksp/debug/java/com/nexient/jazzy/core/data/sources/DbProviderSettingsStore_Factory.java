package com.nexient.jazzy.core.data.sources;

import com.nexient.jazzy.core.data.settings.SecretCipher;
import com.nexient.jazzy.core.database.app.ProviderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nexient.jazzy.core.common.IoDispatcher")
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
public final class DbProviderSettingsStore_Factory implements Factory<DbProviderSettingsStore> {
  private final Provider<ProviderDao> daoProvider;

  private final Provider<SecretCipher> cipherProvider;

  private final Provider<CoroutineDispatcher> ioProvider;

  private DbProviderSettingsStore_Factory(Provider<ProviderDao> daoProvider,
      Provider<SecretCipher> cipherProvider, Provider<CoroutineDispatcher> ioProvider) {
    this.daoProvider = daoProvider;
    this.cipherProvider = cipherProvider;
    this.ioProvider = ioProvider;
  }

  @Override
  public DbProviderSettingsStore get() {
    return newInstance(daoProvider.get(), cipherProvider.get(), ioProvider.get());
  }

  public static DbProviderSettingsStore_Factory create(Provider<ProviderDao> daoProvider,
      Provider<SecretCipher> cipherProvider, Provider<CoroutineDispatcher> ioProvider) {
    return new DbProviderSettingsStore_Factory(daoProvider, cipherProvider, ioProvider);
  }

  public static DbProviderSettingsStore newInstance(ProviderDao dao, SecretCipher cipher,
      CoroutineDispatcher io) {
    return new DbProviderSettingsStore(dao, cipher, io);
  }
}
