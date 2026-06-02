package com.kingkharnivore.skillz.data.di

import com.kingkharnivore.skillz.data.repository.anchor.AnchorRepository
import com.kingkharnivore.skillz.data.repository.anchor.DefaultAnchorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnchorModule {
    @Binds
    @Singleton
    abstract fun bindAnchorRepository(repository: DefaultAnchorRepository): AnchorRepository
}
