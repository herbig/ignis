package com.herbig.ignis.database

import android.app.Application
import androidx.room.Entity
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface LocationDB {
    suspend fun saveLocation(latitude: Double, longitude: Double)
    suspend fun clearLocations(ids: List<Int>)
    fun getLocationCountFlow(): Flow<Int>
    fun getAllLocationsFlow(): Flow<List<LocationEntity>>
}

class RoomLocationDB(context: Application): LocationDB {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        LocationDatabase::class.java,
        "location_database"
    ).build()

    private val locationDao = db.locationDao()

    override suspend fun saveLocation(latitude: Double, longitude: Double) {
        val location = LocationEntity(latitude = latitude, longitude = longitude)
        locationDao.saveLocation(location)
    }

    override suspend fun clearLocations(ids: List<Int>) {
        locationDao.clearLocations(ids)
    }

    override fun getLocationCountFlow(): Flow<Int> {
        return locationDao.getLocationCountFlow()
    }

    override fun getAllLocationsFlow(): Flow<List<LocationEntity>> {
        return locationDao.getAllLocationsFlow()
    }
}

@Database(entities = [LocationEntity::class], version = 1, exportSchema = false)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}

@Entity(tableName = "location_table")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double
)

@Dao
interface LocationDao {
    @Insert
    suspend fun saveLocation(location: LocationEntity)

    @Query("SELECT * FROM location_table ORDER BY timestamp DESC")
    fun getAllLocationsFlow(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM location_table")
    fun getLocationCountFlow(): Flow<Int>

    @Query("DELETE FROM location_table WHERE id IN (:ids)")
    suspend fun clearLocations(ids: List<Int>)
}
