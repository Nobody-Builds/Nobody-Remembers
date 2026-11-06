package com.nobodybuilds.remembers.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrailDao {
    @Insert suspend fun insert(trail: Trail): Long
    @Update suspend fun update(trail: Trail)
    @Query("DELETE FROM trails WHERE id = :id") suspend fun deleteTrail(id: Long)
    @Query("UPDATE trails SET name = :name WHERE id = :id") suspend fun rename(id: Long, name: String)

    @Query("SELECT * FROM trails ORDER BY startedAt DESC") fun observeTrails(): Flow<List<Trail>>
    @Query("SELECT * FROM trails WHERE id = :id") fun observeTrail(id: Long): Flow<Trail?>
    @Query("SELECT * FROM trails WHERE id = :id") suspend fun trail(id: Long): Trail?
    @Query("SELECT * FROM trails WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeUnfinished(): Flow<Trail?>

    @Insert suspend fun insert(point: TrackPoint)
    @Query("SELECT * FROM points WHERE trailId = :trailId ORDER BY time, id") fun observePoints(trailId: Long): Flow<List<TrackPoint>>
    @Query("SELECT * FROM points WHERE trailId = :trailId ORDER BY time, id") suspend fun points(trailId: Long): List<TrackPoint>
    @Query("SELECT * FROM points WHERE trailId = :trailId ORDER BY time DESC, id DESC LIMIT 1") suspend fun lastPoint(trailId: Long): TrackPoint?

    @Insert suspend fun insert(mark: Mark): Long
    @Delete suspend fun delete(mark: Mark)
    @Query("SELECT * FROM marks WHERE trailId = :trailId ORDER BY time") fun observeMarks(trailId: Long): Flow<List<Mark>>
    @Query("SELECT * FROM marks WHERE trailId = :trailId ORDER BY time") suspend fun marks(trailId: Long): List<Mark>
}
