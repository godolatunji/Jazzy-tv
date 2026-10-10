package com.nexient.jazzy.feature.player;

import com.nexient.jazzy.core.data.catalog.TitleRepository;
import com.nexient.jazzy.core.data.library.LibraryRepository;
import com.nexient.jazzy.core.data.library.NextEpisodeResolver;
import com.nexient.jazzy.core.data.profile.ProfileRepository;
import com.nexient.jazzy.core.data.sources.SourceRepository;
import com.nexient.jazzy.core.model.PlaybackRequest;
import com.nexient.jazzy.core.player.PlayerHolder;
import com.nexient.jazzy.core.torrent.LoopbackStreamServer;
import com.nexient.jazzy.core.torrent.TorrentEngine;
import com.nexient.jazzy.core.torrent.TorrentFileProvider;
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
public final class PlayerViewModel_Factory {
  private final Provider<TorrentEngine> engineProvider;

  private final Provider<TorrentFileProvider> filesProvider;

  private final Provider<PlayerHolder> holderProvider;

  private final Provider<LibraryRepository> libraryProvider;

  private final Provider<SourceRepository> sourcesRepoProvider;

  private final Provider<TitleRepository> titlesProvider;

  private final Provider<ProfileRepository> profilesProvider;

  private final Provider<NextEpisodeResolver> nextEpisodesProvider;

  private final Provider<LoopbackStreamServer> loopbackProvider;

  private PlayerViewModel_Factory(Provider<TorrentEngine> engineProvider,
      Provider<TorrentFileProvider> filesProvider, Provider<PlayerHolder> holderProvider,
      Provider<LibraryRepository> libraryProvider, Provider<SourceRepository> sourcesRepoProvider,
      Provider<TitleRepository> titlesProvider, Provider<ProfileRepository> profilesProvider,
      Provider<NextEpisodeResolver> nextEpisodesProvider,
      Provider<LoopbackStreamServer> loopbackProvider) {
    this.engineProvider = engineProvider;
    this.filesProvider = filesProvider;
    this.holderProvider = holderProvider;
    this.libraryProvider = libraryProvider;
    this.sourcesRepoProvider = sourcesRepoProvider;
    this.titlesProvider = titlesProvider;
    this.profilesProvider = profilesProvider;
    this.nextEpisodesProvider = nextEpisodesProvider;
    this.loopbackProvider = loopbackProvider;
  }

  public PlayerViewModel get(PlaybackRequest initial) {
    return newInstance(initial, engineProvider.get(), filesProvider.get(), holderProvider.get(), libraryProvider.get(), sourcesRepoProvider.get(), titlesProvider.get(), profilesProvider.get(), nextEpisodesProvider.get(), loopbackProvider.get());
  }

  public static PlayerViewModel_Factory create(Provider<TorrentEngine> engineProvider,
      Provider<TorrentFileProvider> filesProvider, Provider<PlayerHolder> holderProvider,
      Provider<LibraryRepository> libraryProvider, Provider<SourceRepository> sourcesRepoProvider,
      Provider<TitleRepository> titlesProvider, Provider<ProfileRepository> profilesProvider,
      Provider<NextEpisodeResolver> nextEpisodesProvider,
      Provider<LoopbackStreamServer> loopbackProvider) {
    return new PlayerViewModel_Factory(engineProvider, filesProvider, holderProvider, libraryProvider, sourcesRepoProvider, titlesProvider, profilesProvider, nextEpisodesProvider, loopbackProvider);
  }

  public static PlayerViewModel newInstance(PlaybackRequest initial, TorrentEngine engine,
      TorrentFileProvider files, PlayerHolder holder, LibraryRepository library,
      SourceRepository sourcesRepo, TitleRepository titles, ProfileRepository profiles,
      NextEpisodeResolver nextEpisodes, LoopbackStreamServer loopback) {
    return new PlayerViewModel(initial, engine, files, holder, library, sourcesRepo, titles, profiles, nextEpisodes, loopback);
  }
}
