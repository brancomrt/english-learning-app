package com.englishlearning.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Application-level Hilt module.
 *
 * Placeholder for top-level bindings. Database and repository bindings
 * will be provided in [DatabaseModule] and [RepositoryModule] respectively.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
