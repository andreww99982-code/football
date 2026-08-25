package com.rmatch.football.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw provider payload cache. Only provider responses are stored here —
 * the API key is never written to the database.
 */
@Entity(tableName = "api_cache")
data class ApiCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val fetchedAtMillis: Long
)
