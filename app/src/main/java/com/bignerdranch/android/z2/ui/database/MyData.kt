package com.bignerdranch.android.z2.ui.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "myTable")
data class MyData(
    @PrimaryKey(autoGenerate = true)
    val PrimaryKey: Int,
    val image: ByteArray?,
    val name: String,
    val surname: String,
    val group: String
)
