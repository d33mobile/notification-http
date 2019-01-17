package de.jl.notificationlog.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import android.content.Context
import de.jl.notificationlog.data.item.ActiveNotificationItem
import de.jl.notificationlog.data.item.NotificationItem

@androidx.room.Database(
        version = 4,
        entities = [
            NotificationItem::class,
            ActiveNotificationItem::class
        ]
)
abstract class AppDatabase: RoomDatabase(), Database {
    companion object {
        private val lock = Object()
        private var instance: AppDatabase? = null

        fun with(context: Context): AppDatabase {
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
                                },
                                object : Migration(2, 3) {
                                    override fun migrate(database: SupportSQLiteDatabase) {
                                        // add indexes
                                        database.execSQL("CREATE  INDEX `notifications_index_time` ON `notifications` (`time`)")
                                        database.execSQL("CREATE  INDEX `notifications_index_app_and_time` ON `notifications` (`package`, `time`)")
                                    }
                                },
                                object: Migration(3, 4) {
                                    override fun migrate(database: SupportSQLiteDatabase) {
                                        // add new columns
                                        database.execSQL("ALTER TABLE `notifications` ADD COLUMN `is_oldest_version` INTEGER NOT NULL DEFAULT 1")
                                        database.execSQL("ALTER TABLE `notifications` ADD COLUMN `is_newest_version` INTEGER NOT NULL DEFAULT 1")

                                        // add new table
                                        database.execSQL("CREATE TABLE IF NOT EXISTS `active_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `app_package_name` TEXT NOT NULL, `system_id` INTEGER NOT NULL, `system_tag` TEXT, `previous_notification_item_id` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`previous_notification_item_id`) REFERENCES `notifications`(`id`) ON UPDATE CASCADE ON DELETE CASCADE)")

                                        // add new indexes
                                        database.execSQL("CREATE  INDEX `active_notifications_query_index` ON `active_notifications` (`app_package_name`, `system_id`, `system_tag`)")
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
