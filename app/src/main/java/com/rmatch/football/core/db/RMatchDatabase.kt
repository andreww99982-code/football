package com.rmatch.football.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ApiCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RMatchDatabase : RoomDatabase() {

    abstract fun apiCacheDao(): ApiCacheDao

    companion object {
        private const val NAME = "rmatch_cache.db"

        fun build(context: Context): RMatchDatabase =
            Room.databaseBuilder(context.applicationContext, RMatchDatabase::class.java, NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
