package com.nexient.jazzy.core.data.work;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = CacheEvictionWorker.class
)
public interface CacheEvictionWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.nexient.jazzy.core.data.work.CacheEvictionWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(
      CacheEvictionWorker_AssistedFactory factory);
}
