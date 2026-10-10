package com.nexient.jazzy.core.data.recommendations;

import com.nexient.jazzy.core.data.library.LibraryRepository;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nexient.jazzy.core.common.ApplicationScope")
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
public final class RecommendationTriggers_Factory implements Factory<RecommendationTriggers> {
  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<RecommendationRepository> recommendationsProvider;

  private final Provider<CoroutineScope> scopeProvider;

  private RecommendationTriggers_Factory(Provider<LibraryRepository> libraryProvider,
      Provider<ProfileRepository> profilesProvider,
      Provider<RecommendationRepository> recommendationsProvider,
      Provider<CoroutineScope> scopeProvider) {
    this.libraryProvider = libraryProvider;
    this.profilesProvider = profilesProvider;
    this.recommendationsProvider = recommendationsProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public RecommendationTriggers get() {
    return newInstance(libraryProvider.get(), profilesProvider.get(), recommendationsProvider.get(), scopeProvider.get());
  }

  public static RecommendationTriggers_Factory create(Provider<LibraryRepository> libraryProvider,
      Provider<ProfileRepository> profilesProvider,
      Provider<RecommendationRepository> recommendationsProvider,
      Provider<CoroutineScope> scopeProvider) {
    return new RecommendationTriggers_Factory(libraryProvider, profilesProvider, recommendationsProvider, scopeProvider);
  }

  public static RecommendationTriggers newInstance(LibraryRepository library,
      ProfileRepository profiles, RecommendationRepository recommendations, CoroutineScope scope) {
    return new RecommendationTriggers(library, profiles, recommendations, scope);
  }
}
