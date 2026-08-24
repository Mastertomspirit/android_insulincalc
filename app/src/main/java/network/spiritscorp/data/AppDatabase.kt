package network.spiritscorp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings

@Database(
    entities = [CalculationLog::class, UserSettings::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calculationLogDao(): CalculationLogDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 schema adjustments if needed
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calculation_logs ADD COLUMN beValue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE calculation_logs ADD COLUMN keValue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN beGramsDivisor INTEGER NOT NULL DEFAULT 12")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN showDisclaimer INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "insulin_calculator.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
