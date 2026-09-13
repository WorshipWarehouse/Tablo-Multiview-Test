package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.TabloChannel

@Entity(tableName = "cached_channels")
data class CachedChannelEntity(
    @PrimaryKey
    val id: String,
    val major: Int,
    val minor: Int,
    val network: String,
    val callSign: String,
    val resolution: String?,
    val audio: String?,
    val logoUrl: String?
) {
    fun toDomain(): TabloChannel = TabloChannel(
        id = id,
        major = major,
        minor = minor,
        network = network,
        callSign = callSign,
        resolution = resolution,
        audio = audio,
        logoUrl = logoUrl
    )

    companion object {
        fun fromDomain(channel: TabloChannel): CachedChannelEntity = CachedChannelEntity(
            id = channel.id,
            major = channel.major,
            minor = channel.minor,
            network = channel.network,
            callSign = channel.callSign,
            resolution = channel.resolution,
            audio = channel.audio,
            logoUrl = channel.logoUrl
        )
    }
}
