package com.example.messengerwrapper

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BrowserDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentHistory(): List<HistoryItem>

    @Query("SELECT * FROM history WHERE isBookmark = 1 ORDER BY timestamp DESC")
    suspend fun getBookmarks(): List<HistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HistoryItem)

    @Delete
    suspend fun deleteItem(item: HistoryItem)

    @Query("DELETE FROM history WHERE isBookmark = 0")
    suspend fun clearHistory()
}
