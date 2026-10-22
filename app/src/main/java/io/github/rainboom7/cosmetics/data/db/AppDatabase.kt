package io.github.rainboom7.cosmetics.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ItemEntity::class, UsageEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun items(): ItemDao

    abstract fun usage(): UsageDao

    companion object {
        /** Version 2: status marks, favorites, finish date and the usage calendar. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN shelfStatus TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN finishedOn INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS usage_log (itemId INTEGER NOT NULL, day INTEGER NOT NULL, " +
                        "PRIMARY KEY(itemId, day), " +
                        "FOREIGN KEY(itemId) REFERENCES items(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_usage_log_day ON usage_log (day)")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cosmetics.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
