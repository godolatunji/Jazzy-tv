package com.nexient.jazzy.core.database;

import android.content.Context;
import com.nexient.jazzy.core.database.app.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_AppDatabaseFactory implements Factory<AppDatabase> {
  private final Provider<Context> contextProvider;

  private DatabaseModule_AppDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppDatabase get() {
    return appDatabase(contextProvider.get());
  }

  public static DatabaseModule_AppDatabaseFactory create(Provider<Context> contextProvider) {
    return new DatabaseModule_AppDatabaseFactory(contextProvider);
  }

  public static AppDatabase appDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.appDatabase(context));
  }
}
