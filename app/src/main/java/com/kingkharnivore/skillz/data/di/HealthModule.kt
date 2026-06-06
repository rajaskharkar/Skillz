package com.kingkharnivore.skillz.data.di

import com.kingkharnivore.skillz.domain.health.MovementBonusCalculator
import com.kingkharnivore.skillz.domain.health.MovementBonusEligibilityPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {
    @Provides fun provideMovementBonusCalculator(): MovementBonusCalculator = MovementBonusCalculator()
    @Provides fun provideMovementBonusEligibilityPolicy(): MovementBonusEligibilityPolicy = MovementBonusEligibilityPolicy()
}
