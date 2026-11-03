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
    version = 4,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `study_groups_new` (`id` TEXT NOT NULL, `localUserId` TEXT NOT NULL, `name` TEXT NOT NULL, `subject` TEXT NOT NULL, `description` TEXT NOT NULL, `ownerId` TEXT NOT NULL, `memberIds` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`, `localUserId`))")
                db.execSQL("INSERT INTO `study_groups_new` (`id`, `localUserId`, `name`, `subject`, `description`, `ownerId`, `memberIds`, `iconKey`, `createdAt`) SELECT `id`, `localUserId`, `name`, `subject`, `description`, `ownerId`, `memberIds`, `iconKey`, `createdAt` FROM `study_groups`")
                db.execSQL("DROP TABLE `study_groups`")
                db.execSQL("ALTER TABLE `study_groups_new` RENAME TO `study_groups`")
                db.execSQL("CREATE TABLE `tasks_new` (`id` TEXT NOT NULL, `localUserId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `dueDate` INTEGER NOT NULL, `assignedToId` TEXT NOT NULL, `assignedToName` TEXT NOT NULL, `groupId` TEXT NOT NULL, `priority` TEXT NOT NULL, `completed` INTEGER NOT NULL, `ownerId` TEXT NOT NULL, PRIMARY KEY(`id`, `localUserId`))")
                db.execSQL("INSERT INTO `tasks_new` (`id`, `localUserId`, `title`, `description`, `dueDate`, `assignedToId`, `assignedToName`, `groupId`, `priority`, `completed`, `ownerId`) SELECT `id`, `localUserId`, `title`, `description`, `dueDate`, `assignedToId`, `assignedToName`, `groupId`, `priority`, `completed`, `ownerId` FROM `tasks`")
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
                db.execSQL("CREATE TABLE `study_sessions_new` (`id` TEXT NOT NULL, `localUserId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `groupId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `location` TEXT NOT NULL, `attendeeIds` TEXT NOT NULL, `ownerId` TEXT NOT NULL, PRIMARY KEY(`id`, `localUserId`))")
                db.execSQL("INSERT INTO `study_sessions_new` (`id`, `localUserId`, `title`, `description`, `groupId`, `startTime`, `endTime`, `location`, `attendeeIds`, `ownerId`) SELECT `id`, `localUserId`, `title`, `description`, `groupId`, `startTime`, `endTime`, `location`, `attendeeIds`, `ownerId` FROM `study_sessions`")
                db.execSQL("DROP TABLE `study_sessions`")
                db.execSQL("ALTER TABLE `study_sessions_new` RENAME TO `study_sessions`")
                db.execSQL("CREATE TABLE `chat_messages_new` (`id` TEXT NOT NULL, `localUserId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `text` TEXT NOT NULL, `sentAt` INTEGER NOT NULL, PRIMARY KEY(`id`, `localUserId`))")
                db.execSQL("INSERT INTO `chat_messages_new` (`id`, `localUserId`, `groupId`, `senderId`, `senderName`, `text`, `sentAt`) SELECT `id`, `localUserId`, `groupId`, `senderId`, `senderName`, `text`, `sentAt` FROM `chat_messages`")
                db.execSQL("DROP TABLE `chat_messages`")
                db.execSQL("ALTER TABLE `chat_messages_new` RENAME TO `chat_messages`")
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
