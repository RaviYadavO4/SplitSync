package com.portfolio.splitsync.di

import android.content.Context
import androidx.room.Room
import com.portfolio.splitsync.data.local.SplitSyncDatabase
import com.portfolio.splitsync.data.local.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SplitSyncDatabase {
        return Room.databaseBuilder(
            context,
            SplitSyncDatabase::class.java,
            "splitsync_offline_db"
        )
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides
    fun provideExpenseDao(database: SplitSyncDatabase): ExpenseDao {
        return database.expenseDao()
    }
}
