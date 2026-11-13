package com.portfolio.splitsync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.portfolio.splitsync.data.local.dao.ExpenseDao
import com.portfolio.splitsync.data.local.entity.ExpenseEntity
import com.portfolio.splitsync.data.local.entity.ExpenseSplitEntity
import com.portfolio.splitsync.data.local.entity.GroupEntity

@Database(
    entities = [
        GroupEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SplitSyncDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}
