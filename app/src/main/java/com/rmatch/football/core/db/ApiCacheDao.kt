package com.rmatch.football.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiCacheDao {

    @Query("SELECT * FROM api_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun find(key: String): ApiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ApiCacheEntity)

    @Query("DELETE FROM api_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM api_cache")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM api_cache")
    suspend fun count(): Int

    @Query("SELECT MAX(fetchedAtMillis) FROM api_cache")
    suspend fun lastUpdate(): Long?
}
