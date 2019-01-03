package de.jl.notificationlog.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import android.content.Context

@androidx.room.Database(
        version = 2,
        entities = [
            NotificationItem::class
        ]
)
abstract class AppDatabase: RoomDatabase(), Database {
    companion object {
        private val lock = Object()
        private var instance: Database? = null

        fun with(context: Context): Database {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "data.db"
                        ).addMigrations(
                                object: Migration(1, 2) {
                                    override fun migrate(database: SupportSQLiteDatabase) {
                                        // create new table
                                        database.execSQL("CREATE TABLE IF NOT EXISTS `notifications_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `package` TEXT NOT NULL, `time` INTEGER NOT NULL, `title` TEXT NOT NULL, `text` TEXT NOT NULL)")

                                        // migrate the data
                                        database.execSQL("INSERT INTO notifications_new (id, package, time, title, text) SELECT _id, package, notification_time, notification_title, notification_text FROM notifications");

                                        // replace the old table by the new one
                                        database.execSQL("DROP TABLE notifications")
                                        database.execSQL("ALTER TABLE notifications_new RENAME TO notifications")
                                    }
                                }
                        ).build()
                    }
                }
            }

            return instance!!
        }
    }
}
