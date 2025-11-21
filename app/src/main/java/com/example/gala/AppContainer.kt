package com.example.gala

import android.content.Context
import com.example.gala.data.database.DatabaseModule
import com.example.gala.data.repository.GalaRepository

class AppContainer(context: Context) {

    private val db = DatabaseModule.db(context)

    val repository = GalaRepository(db)
}
