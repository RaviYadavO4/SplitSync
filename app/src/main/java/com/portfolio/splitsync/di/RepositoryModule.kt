package com.portfolio.splitsync.di

import com.portfolio.splitsync.data.repository.AuthRepositoryImpl
import com.portfolio.splitsync.data.repository.ExpenseRepositoryImpl
import com.portfolio.splitsync.domain.repository.AuthRepository
import com.portfolio.splitsync.domain.repository.ExpenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}
