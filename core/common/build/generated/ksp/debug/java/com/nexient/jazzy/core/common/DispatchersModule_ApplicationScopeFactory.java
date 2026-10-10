package com.nexient.jazzy.core.common;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "com.nexient.jazzy.core.common.ApplicationScope",
    "com.nexient.jazzy.core.common.DefaultDispatcher"
})
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
public final class DispatchersModule_ApplicationScopeFactory implements Factory<CoroutineScope> {
  private final Provider<CoroutineDispatcher> dispatcherProvider;

  private DispatchersModule_ApplicationScopeFactory(
      Provider<CoroutineDispatcher> dispatcherProvider) {
    this.dispatcherProvider = dispatcherProvider;
  }

  @Override
  public CoroutineScope get() {
    return applicationScope(dispatcherProvider.get());
  }

  public static DispatchersModule_ApplicationScopeFactory create(
      Provider<CoroutineDispatcher> dispatcherProvider) {
    return new DispatchersModule_ApplicationScopeFactory(dispatcherProvider);
  }

  public static CoroutineScope applicationScope(CoroutineDispatcher dispatcher) {
    return Preconditions.checkNotNullFromProvides(DispatchersModule.INSTANCE.applicationScope(dispatcher));
  }
}
