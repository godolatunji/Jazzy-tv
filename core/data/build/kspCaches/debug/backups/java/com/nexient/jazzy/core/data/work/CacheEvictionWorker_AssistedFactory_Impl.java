package com.nexient.jazzy.core.data.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CacheEvictionWorker_AssistedFactory_Impl implements CacheEvictionWorker_AssistedFactory {
  private final CacheEvictionWorker_Factory delegateFactory;

  CacheEvictionWorker_AssistedFactory_Impl(CacheEvictionWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public CacheEvictionWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<CacheEvictionWorker_AssistedFactory> create(
      CacheEvictionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new CacheEvictionWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<CacheEvictionWorker_AssistedFactory> createFactoryProvider(
      CacheEvictionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new CacheEvictionWorker_AssistedFactory_Impl(delegateFactory));
  }
}
