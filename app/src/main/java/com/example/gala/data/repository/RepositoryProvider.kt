package com.example.gala.data.repository

import android.content.Context
import com.example.gala.data.database.DatabaseModule

object RepositoryProvider {
    @Volatile
    private var instance: GalaRepository? = null

    fun get(context: Context): GalaRepository {
        return instance ?: synchronized(this) {
            instance ?: GalaRepository(
                db = DatabaseModule.db(context)
            ).also { instance = it }
        }
    }
}