package com.nexient.jazzy.core.common;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata
@QualifierMetadata("com.nexient.jazzy.core.common.DefaultDispatcher")
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
public final class DispatchersModule_DefaultDispatcherFactory implements Factory<CoroutineDispatcher> {
  @Override
  public CoroutineDispatcher get() {
    return defaultDispatcher();
  }

  public static DispatchersModule_DefaultDispatcherFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CoroutineDispatcher defaultDispatcher() {
    return Preconditions.checkNotNullFromProvides(DispatchersModule.INSTANCE.defaultDispatcher());
  }

  private static final class InstanceHolder {
    static final DispatchersModule_DefaultDispatcherFactory INSTANCE = new DispatchersModule_DefaultDispatcherFactory();
  }
}
