package com.nexient.jazzy.core.data.device;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DeviceCapabilities_Factory implements Factory<DeviceCapabilities> {
  private final Provider<Context> contextProvider;

  private DeviceCapabilities_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DeviceCapabilities get() {
    return newInstance(contextProvider.get());
  }

  public static DeviceCapabilities_Factory create(Provider<Context> contextProvider) {
    return new DeviceCapabilities_Factory(contextProvider);
  }

  public static DeviceCapabilities newInstance(Context context) {
    return new DeviceCapabilities(context);
  }
}
