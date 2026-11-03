package com.studyhub.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studyhub.app.data.local.StudyHubDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountCacheMigrationTest {
    @Test fun migrationPreservesRowsAndAllowsSameTaskForTwoAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-regression.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE study_groups (id TEXT NOT NULL PRIMARY KEY, localUserId TEXT NOT NULL, name TEXT NOT NULL, subject TEXT NOT NULL, description TEXT NOT NULL, ownerId TEXT NOT NULL, memberIds TEXT NOT NULL, iconKey TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE tasks (id TEXT NOT NULL PRIMARY KEY, localUserId TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, dueDate INTEGER NOT NULL, assignedToId TEXT NOT NULL, assignedToName TEXT NOT NULL, groupId TEXT NOT NULL, priority TEXT NOT NULL, completed INTEGER NOT NULL, ownerId TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE study_sessions (id TEXT NOT NULL PRIMARY KEY, localUserId TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, groupId TEXT NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER NOT NULL, location TEXT NOT NULL, attendeeIds TEXT NOT NULL, ownerId TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE chat_messages (id TEXT NOT NULL PRIMARY KEY, localUserId TEXT NOT NULL, groupId TEXT NOT NULL, senderId TEXT NOT NULL, senderName TEXT NOT NULL, text TEXT NOT NULL, sentAt INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO tasks VALUES ('task','alice','Study','',100,'alice','Alice','group','HIGH',0,'alice')")
            StudyHubDatabase.MIGRATION_3_4.migrate(db)
            db.execSQL("INSERT INTO tasks SELECT id,'bob',title,description,dueDate,assignedToId,assignedToName,groupId,priority,completed,ownerId FROM tasks WHERE localUserId='alice'")
            db.query("SELECT COUNT(*) FROM tasks WHERE id='task'").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        } finally { helper.close(); context.deleteDatabase(name) }
    }
}
