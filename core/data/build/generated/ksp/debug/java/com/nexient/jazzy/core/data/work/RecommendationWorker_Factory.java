package com.nexient.jazzy.core.data.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.recommendations.RecommendationRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Provider;
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
public final class RecommendationWorker_Factory {
  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<RecommendationRepository> recommendationsProvider;

  private RecommendationWorker_Factory(Provider<ProfileRepository> profilesProvider,
      Provider<RecommendationRepository> recommendationsProvider) {
    this.profilesProvider = profilesProvider;
    this.recommendationsProvider = recommendationsProvider;
  }

  public RecommendationWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, profilesProvider.get(), recommendationsProvider.get());
  }

  public static RecommendationWorker_Factory create(Provider<ProfileRepository> profilesProvider,
      Provider<RecommendationRepository> recommendationsProvider) {
    return new RecommendationWorker_Factory(profilesProvider, recommendationsProvider);
  }

  public static RecommendationWorker newInstance(Context context, WorkerParameters params,
      ProfileRepository profiles, RecommendationRepository recommendations) {
    return new RecommendationWorker(context, params, profiles, recommendations);
  }
}
