package com.pxy.depositbook.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pxy.depositbook.data.dao.AssetSnapshotDao
import com.pxy.depositbook.data.dao.DepositDao
import com.pxy.depositbook.data.dao.OperationLogDao
import com.pxy.depositbook.data.entity.AssetSnapshot
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.entity.OperationLog

@Database(
    entities = [Deposit::class, OperationLog::class, AssetSnapshot::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun depositDao(): DepositDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun assetSnapshotDao(): AssetSnapshotDao

    companion object {
        /** v1 -> v2：新增 asset_snapshot 历史资产快照表 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `asset_snapshot` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`day` TEXT NOT NULL, " +
                            "`totalPrincipal` REAL NOT NULL, " +
                            "`totalInterest` REAL NOT NULL, " +
                            "`totalValue` REAL NOT NULL, " +
                            "`activeCount` INTEGER NOT NULL, " +
                            "`recordedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_asset_snapshot_day` ON `asset_snapshot` (`day`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deposit_book.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
