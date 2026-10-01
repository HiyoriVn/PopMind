package com.example.popmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActiveFocusDao {
    @Query("SELECT * FROM active_focus WHERE id = 1")
    suspend fun getActive(): ActiveFocusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: ActiveFocusEntity)

    @Query("DELETE FROM active_focus WHERE id = 1")
    suspend fun clear()
}
