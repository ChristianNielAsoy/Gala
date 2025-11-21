package com.example.gala.data.database

import android.content.Context
import androidx.room.Room

object DatabaseModule {
    @Volatile private var instance: GalaDatabase? = null

    fun db(context: Context): GalaDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                GalaDatabase::class.java,
                "gala.db"
            ).fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
}
