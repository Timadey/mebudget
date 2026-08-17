package com.mebudget.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toNegativeBalanceRule(value: String?): NegativeBalanceRule? = value?.let(NegativeBalanceRule::valueOf)

    @TypeConverter
    fun fromNegativeBalanceRule(value: NegativeBalanceRule?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let(TransactionType::valueOf)

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name
}

@Database(
    entities = [
        BudgetEntity::class,
        WalletEntity::class,
        TransactionEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mebudget.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }

        val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_metadata` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `entityType` TEXT NOT NULL,
                    `localId` INTEGER NOT NULL,
                    `remoteId` TEXT,
                    `deleted` INTEGER NOT NULL,
                    `lastSyncedAtMillis` INTEGER,
                    `lastError` TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_metadata_localId ON sync_metadata (localId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_metadata_remoteId ON sync_metadata (remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_metadata_entityType ON sync_metadata (entityType)")
        }
    }
}

