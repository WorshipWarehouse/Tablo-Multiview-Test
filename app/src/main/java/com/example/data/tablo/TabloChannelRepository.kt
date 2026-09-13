package com.example.data.tablo

import com.example.data.local.CachedChannelDao
import com.example.data.local.CachedChannelEntity
import com.example.model.TabloAiring
import com.example.model.TabloChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages live TV channels and EPG guide data.
 */
class TabloChannelRepository(
    private val apiClient: TabloApiClient,
    private val channelDao: CachedChannelDao
) {
    private val _channels = MutableStateFlow<List<TabloChannel>>(emptyList())
    val channels: StateFlow<List<TabloChannel>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _airingsMap = MutableStateFlow<Map<String, TabloAiring>>(emptyMap())
    val airingsMap: StateFlow<Map<String, TabloAiring>> = _airingsMap.asStateFlow()

    /**
     * Load cached channels from local Room database for fast instant start.
     */
    suspend fun loadCachedChannels(): List<TabloChannel> = withContext(Dispatchers.IO) {
        val cached = channelDao.getAllChannelsList().map { it.toDomain() }
        if (cached.isNotEmpty()) {
            _channels.value = cached
        }
        cached
    }

    /**
     * Refresh channels from physical Tablo device over LAN.
     */
    suspend fun refreshChannels(host: String, port: Int = 8885): Result<List<TabloChannel>> =
        withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = apiClient.getChannels(host, port)
                if (result.isSuccess) {
                    val list = result.getOrThrow()
                    if (list.isNotEmpty()) {
                        _channels.value = list
                        // Persist to Room cache
                        val entities = list.map { CachedChannelEntity.fromDomain(it) }
                        channelDao.clearAll()
                        channelDao.insertChannels(entities)
                    }
                    Result.success(list)
                } else {
                    // Fallback to local Room cache
                    val fallback = channelDao.getAllChannelsList().map { it.toDomain() }
                    if (fallback.isNotEmpty()) {
                        _channels.value = fallback
                        Result.success(fallback)
                    } else {
                        result
                    }
                }
            } catch (e: Exception) {
                val fallback = channelDao.getAllChannelsList().map { it.toDomain() }
                if (fallback.isNotEmpty()) {
                    _channels.value = fallback
                    Result.success(fallback)
                } else {
                    Result.failure(e)
                }
            } finally {
                _isLoading.value = false
            }
        }

    /**
     * Fetch current program airing for a channel.
     */
    suspend fun loadCurrentAiring(host: String, channelId: String, port: Int = 8885): TabloAiring? =
        withContext(Dispatchers.IO) {
            val result = apiClient.getChannelAirings(host, channelId, port)
            val airing = result.getOrNull()?.firstOrNull()
            if (airing != null) {
                _airingsMap.value = _airingsMap.value + (channelId to airing)
            }
            airing
        }
}
