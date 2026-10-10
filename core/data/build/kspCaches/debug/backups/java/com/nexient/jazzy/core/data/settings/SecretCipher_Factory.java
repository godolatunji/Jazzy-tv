package com.nexient.jazzy.core.data.settings;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SecretCipher_Factory implements Factory<SecretCipher> {
  @Override
  public SecretCipher get() {
    return newInstance();
  }

  public static SecretCipher_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SecretCipher newInstance() {
    return new SecretCipher();
  }

  private static final class InstanceHolder {
    static final SecretCipher_Factory INSTANCE = new SecretCipher_Factory();
  }
}
