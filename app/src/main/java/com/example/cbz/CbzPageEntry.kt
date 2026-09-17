package com.example.cbz

data class CbzPageEntry(
    val index: Int, // 0-based index
    val entryName: String,
    val displayName: String,
    val size: Long = 0L
)
