package com.nexient.jazzy.core.common;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SystemClock_Factory implements Factory<SystemClock> {
  @Override
  public SystemClock get() {
    return newInstance();
  }

  public static SystemClock_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SystemClock newInstance() {
    return new SystemClock();
  }

  private static final class InstanceHolder {
    static final SystemClock_Factory INSTANCE = new SystemClock_Factory();
  }
}
