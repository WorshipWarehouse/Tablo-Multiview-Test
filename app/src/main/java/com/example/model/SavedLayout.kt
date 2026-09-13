package com.example.model

data class SavedLayout(
    val id: Long = 0L,
    val name: String,
    val mode: MultiviewLayoutMode,
    val channelIds: List<String>,
    val channelLabels: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
