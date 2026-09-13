package com.example.data.tablo

import com.example.data.local.SavedLayoutDao
import com.example.data.local.SavedLayoutEntity
import com.example.model.MultiviewLayoutMode
import com.example.model.SavedLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SavedLayoutRepository(
    private val dao: SavedLayoutDao
) {
    val layouts: Flow<List<SavedLayout>> = dao.getAllLayouts().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveLayout(
        name: String,
        mode: MultiviewLayoutMode,
        channelIds: List<String>,
        channelLabels: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val layout = SavedLayout(
            name = name,
            mode = mode,
            channelIds = channelIds,
            channelLabels = channelLabels
        )
        dao.insertLayout(SavedLayoutEntity.fromDomain(layout))
    }

    suspend fun deleteLayout(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun renameLayout(layout: SavedLayout, newName: String) = withContext(Dispatchers.IO) {
        val updated = layout.copy(name = newName)
        dao.updateLayout(SavedLayoutEntity.fromDomain(updated))
    }
}
