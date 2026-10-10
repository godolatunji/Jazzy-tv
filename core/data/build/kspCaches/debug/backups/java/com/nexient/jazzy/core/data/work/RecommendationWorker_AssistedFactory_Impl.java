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
public final class RecommendationWorker_AssistedFactory_Impl implements RecommendationWorker_AssistedFactory {
  private final RecommendationWorker_Factory delegateFactory;

  RecommendationWorker_AssistedFactory_Impl(RecommendationWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public RecommendationWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<RecommendationWorker_AssistedFactory> create(
      RecommendationWorker_Factory delegateFactory) {
    return InstanceFactory.create(new RecommendationWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<RecommendationWorker_AssistedFactory> createFactoryProvider(
      RecommendationWorker_Factory delegateFactory) {
    return InstanceFactory.create(new RecommendationWorker_AssistedFactory_Impl(delegateFactory));
  }
}
