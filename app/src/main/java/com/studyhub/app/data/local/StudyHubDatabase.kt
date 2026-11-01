package com.studyhub.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        StudyGroupEntity::class,
        TaskEntity::class,
        StudySessionEntity::class,
        ChatMessageEntity::class,
        PendingTaskOperationEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyHubDatabase : RoomDatabase() {
    abstract fun dao(): StudyHubDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `pending_task_operations` (
                        `operationId` TEXT NOT NULL,
                        `localUserId` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`operationId`)
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pending_task_operations_localUserId_createdAt` " +
                        "ON `pending_task_operations` (`localUserId`, `createdAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pending_task_operations_taskId` " +
                        "ON `pending_task_operations` (`taskId`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: StudyHubDatabase? = null

        fun getDatabase(context: Context): StudyHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyHubDatabase::class.java,
                    "study_hub_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
