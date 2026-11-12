package com.dstwr.dayflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, NoteEntity::class, ReminderEntity::class], version = 1, exportSchema = true)
abstract class DayflowDatabase : RoomDatabase() {
    abstract fun dayflowDao(): DayflowDao
}
