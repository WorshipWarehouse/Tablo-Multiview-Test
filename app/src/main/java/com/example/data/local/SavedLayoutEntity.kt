package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MultiviewLayoutMode
import com.example.model.SavedLayout
import org.json.JSONArray

@Entity(tableName = "saved_layouts")
data class SavedLayoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val mode: String,
    val channelIdsJson: String,
    val channelLabelsJson: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SavedLayout {
        val idsList = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(channelIdsJson)
            for (i in 0 until jsonArray.length()) {
                idsList.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {}

        val labelsList = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(channelLabelsJson)
            for (i in 0 until jsonArray.length()) {
                labelsList.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {}

        val layoutMode = try {
            MultiviewLayoutMode.valueOf(mode)
        } catch (_: Exception) {
            MultiviewLayoutMode.FOUR_PANE
        }

        return SavedLayout(
            id = id,
            name = name,
            mode = layoutMode,
            channelIds = idsList,
            channelLabels = labelsList,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(layout: SavedLayout): SavedLayoutEntity {
            val idsArray = JSONArray()
            layout.channelIds.forEach { idsArray.put(it) }
            val labelsArray = JSONArray()
            layout.channelLabels.forEach { labelsArray.put(it) }

            return SavedLayoutEntity(
                id = layout.id,
                name = layout.name,
                mode = layout.mode.name,
                channelIdsJson = idsArray.toString(),
                channelLabelsJson = labelsArray.toString(),
                createdAt = layout.createdAt
            )
        }
    }
}
