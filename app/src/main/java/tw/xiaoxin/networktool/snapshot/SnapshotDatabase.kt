package tw.xiaoxin.networktool.snapshot

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "diagnostic_snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schemaVersion: Int = 1,
    val capturedAt: Long,
    val label: String,
    val transport: String,
    val underlyingTransport: String?,
    val publicIp: String,
    val validated: Boolean,
    val airplaneMode: Boolean,
    val externalTunnel: Boolean,
    val localAddresses: String,
    val gateway: String?,
    val dnsUpstream: String?,
    val probesJson: String,
    val fastlyJson: String,
)

@Dao
interface SnapshotDao {
    @Insert suspend fun insert(snapshot: SnapshotEntity): Long
    @Query("SELECT * FROM diagnostic_snapshots ORDER BY capturedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 20): List<SnapshotEntity>
    @Query("SELECT * FROM diagnostic_snapshots WHERE label = '變更前' ORDER BY capturedAt DESC LIMIT 1")
    suspend fun latestBaseline(): SnapshotEntity?
    @Query("DELETE FROM diagnostic_snapshots WHERE id NOT IN (SELECT id FROM diagnostic_snapshots ORDER BY capturedAt DESC LIMIT :keep)")
    suspend fun trim(keep: Int = 100)
}

@Database(entities = [SnapshotEntity::class], version = 1, exportSchema = true)
abstract class SnapshotDatabase : RoomDatabase() {
    abstract fun snapshots(): SnapshotDao

    companion object {
        @Volatile private var instance: SnapshotDatabase? = null

        fun get(context: Context): SnapshotDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SnapshotDatabase::class.java,
                "diagnostic-snapshots.db",
            ).build().also { instance = it }
        }
    }
}
