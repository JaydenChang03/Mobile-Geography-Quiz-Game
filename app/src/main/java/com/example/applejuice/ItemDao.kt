package com.example.applejuice
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = (:id)")
    suspend fun getItem(id: UUID): Item?

    @Query("SELECT * FROM items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<Item>>

    @Insert
    suspend fun addItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

}