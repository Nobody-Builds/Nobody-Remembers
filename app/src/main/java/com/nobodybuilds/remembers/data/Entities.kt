package com.nobodybuilds.remembers.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trails")
data class Trail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startedAt: Long,
    /** Null while the trail is still being recorded. */
    val endedAt: Long? = null,
    val distance: Double = 0.0,
    val ascent: Double = 0.0,
    val descent: Double = 0.0,
    val minAlt: Double? = null,
    val maxAlt: Double? = null,
    val barometric: Boolean = false,
)

@Entity(
    tableName = "points",
    foreignKeys = [ForeignKey(Trail::class, ["id"], ["trailId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trailId")],
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trailId: Long,
    val time: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double?,
    val accuracy: Float,
)

enum class MarkType { BIRD, JUNCTION, WATER, VIEW, HAZARD, NOTE }

@Entity(
    tableName = "marks",
    foreignKeys = [ForeignKey(Trail::class, ["id"], ["trailId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trailId")],
)
data class Mark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trailId: Long,
    val time: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double?,
    val type: MarkType,
    val note: String,
)
