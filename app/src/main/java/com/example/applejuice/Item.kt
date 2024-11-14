package com.example.applejuice

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var description: String = "",
    var category: String = "",
    var date: Date = Date(),
    var photoUri: String? = null
)