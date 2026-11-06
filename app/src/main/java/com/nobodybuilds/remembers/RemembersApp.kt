package com.nobodybuilds.remembers

import android.app.Application
import android.content.Context
import com.nobodybuilds.remembers.data.AppDatabase
import com.nobodybuilds.remembers.data.TrailDao

class RemembersApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
}

val Context.trailDao: TrailDao get() = (applicationContext as RemembersApp).database.trails()
