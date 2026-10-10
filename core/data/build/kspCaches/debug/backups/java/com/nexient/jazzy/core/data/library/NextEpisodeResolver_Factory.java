package com.nexient.jazzy.core.data.library;

import com.nexient.jazzy.core.data.catalog.TitleRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class NextEpisodeResolver_Factory implements Factory<NextEpisodeResolver> {
  private final Provider<TitleRepository> titlesProvider;

  private NextEpisodeResolver_Factory(Provider<TitleRepository> titlesProvider) {
    this.titlesProvider = titlesProvider;
  }

  @Override
  public NextEpisodeResolver get() {
    return newInstance(titlesProvider.get());
  }

  public static NextEpisodeResolver_Factory create(Provider<TitleRepository> titlesProvider) {
    return new NextEpisodeResolver_Factory(titlesProvider);
  }

  public static NextEpisodeResolver newInstance(TitleRepository titles) {
    return new NextEpisodeResolver(titles);
  }
}
