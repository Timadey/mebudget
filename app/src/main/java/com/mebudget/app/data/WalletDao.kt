package com.mebudget.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY budgetId ASC, sortOrder ASC, id ASC")
    fun observeAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE budgetId = :budgetId ORDER BY sortOrder ASC, id ASC")
    fun observeWalletsForBudget(budgetId: Long): Flow<List<WalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletEntity>): List<Long>

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    suspend fun getWallet(walletId: Long): WalletEntity?

    @Query("SELECT MAX(sortOrder) FROM wallets WHERE budgetId = :budgetId")
    suspend fun getMaxSortOrder(budgetId: Long): Int?

    @Query("SELECT * FROM wallets WHERE budgetId = :budgetId ORDER BY sortOrder ASC, id ASC")
    suspend fun getWalletsForBudget(budgetId: Long): List<WalletEntity>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWallets(): List<WalletEntity>
}
