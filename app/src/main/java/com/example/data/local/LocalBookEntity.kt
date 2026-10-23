package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity to store book information including title, author,
 * description, and local file path.
 */
@Entity(tableName = "books")
data class LocalBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val author: String = "",
    val description: String = "",
    @ColumnInfo(name = "local_file_path")
    val localFilePath: String = "",
    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,
    @ColumnInfo(name = "mime_type")
    val mimeType: String = "application/pdf",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
