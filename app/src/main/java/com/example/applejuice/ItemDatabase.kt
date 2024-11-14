package com.example.applejuice

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class], version = 2)
@TypeConverters(Converters::class)
abstract class ItemDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var instance: ItemDatabase? = null

        fun get(context: Context): ItemDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): ItemDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ItemDatabase::class.java,
                "item_database"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN category TEXT NOT NULL DEFAULT 'Uncategorized'")
            }
        }
    }
}