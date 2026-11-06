package com.nobodybuilds.remembers.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trail::class, TrackPoint::class, Mark::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trails(): TrailDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "trails.db").build()
    }
}
