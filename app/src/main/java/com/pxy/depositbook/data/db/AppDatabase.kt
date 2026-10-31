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
import com.pxy.depositbook.data.dao.ProfileDao
import com.pxy.depositbook.data.entity.AssetSnapshot
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.entity.OperationLog
import com.pxy.depositbook.data.entity.Profile

@Database(
    entities = [Deposit::class, OperationLog::class, AssetSnapshot::class, Profile::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun depositDao(): DepositDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun assetSnapshotDao(): AssetSnapshotDao
    abstract fun profileDao(): ProfileDao

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

        /** v2 -> v3：多账户。新增 profile 表；deposit/operation_log/asset_snapshot 加 profileId 列（默认 1） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`avatarPath` TEXT, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`sortOrder` INTEGER NOT NULL)"
                )
                // 旧数据全部归入 1 号默认账户
                db.execSQL(
                    "INSERT INTO `profile` (`id`,`name`,`avatarPath`,`createdAt`,`sortOrder`) " +
                            "SELECT 1, '默认账户', NULL, ${System.currentTimeMillis()}, 0 " +
                            "WHERE NOT EXISTS (SELECT 1 FROM `profile` WHERE `id` = 1)"
                )
                db.execSQL("ALTER TABLE `deposit` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `operation_log` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                // 快照表重建：加 profileId，唯一索引从 (day) 变为 (day, profileId)
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `asset_snapshot_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`profileId` INTEGER NOT NULL DEFAULT 1, " +
                            "`day` TEXT NOT NULL, " +
                            "`totalPrincipal` REAL NOT NULL, " +
                            "`totalInterest` REAL NOT NULL, " +
                            "`totalValue` REAL NOT NULL, " +
                            "`activeCount` INTEGER NOT NULL, " +
                            "`recordedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `asset_snapshot_new` (`id`,`profileId`,`day`,`totalPrincipal`,`totalInterest`,`totalValue`,`activeCount`,`recordedAt`) " +
                            "SELECT `id`,1,`day`,`totalPrincipal`,`totalInterest`,`totalValue`,`activeCount`,`recordedAt` FROM `asset_snapshot`"
                )
                db.execSQL("DROP TABLE `asset_snapshot`")
                db.execSQL("ALTER TABLE `asset_snapshot_new` RENAME TO `asset_snapshot`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_asset_snapshot_day_profileId` " +
                            "ON `asset_snapshot` (`day`, `profileId`)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
