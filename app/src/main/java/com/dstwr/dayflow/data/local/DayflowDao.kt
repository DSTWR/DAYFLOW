package com.dstwr.dayflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayflowDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueAt ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTask(task: TaskEntity): Long
    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: NoteEntity): Long
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY triggerAt ASC")
    fun observeReminders(): Flow<List<ReminderEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReminder(reminder: ReminderEntity): Long
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}
