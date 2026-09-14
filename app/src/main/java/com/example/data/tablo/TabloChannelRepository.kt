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
    private val _channels = MutableStateFlow<List<TabloChannel>>(defaultBroadcastChannels())
    val channels: StateFlow<List<TabloChannel>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _airingsMap = MutableStateFlow<Map<String, TabloAiring>>(defaultAiringsMap())
    val airingsMap: StateFlow<Map<String, TabloAiring>> = _airingsMap.asStateFlow()

    /**
     * Load cached channels from local Room database for fast instant start.
     */
    suspend fun loadCachedChannels(): List<TabloChannel> = withContext(Dispatchers.IO) {
        val cached = channelDao.getAllChannelsList().map { it.toDomain() }
        if (cached.isNotEmpty()) {
            _channels.value = cached
            cached
        } else {
            val defaults = defaultBroadcastChannels()
            _channels.value = defaults
            defaults
        }
    }

    /**
     * Refresh channels from Tablo Cloud guide or local physical Tablo device.
     */
    suspend fun refreshChannels(
        host: String,
        port: Int = 8885,
        accessToken: String? = null,
        lighthouseToken: String? = null
    ): Result<List<TabloChannel>> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val result = apiClient.getChannels(host, port, accessToken, lighthouseToken)
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

fun defaultBroadcastChannels(): List<TabloChannel> = listOf(
    // OTA Antenna Channels (Screenshots 1, 2)
    TabloChannel(
        id = "ch_ind_9_1",
        major = 9,
        minor = 1,
        network = "INDEPENDENT",
        callSign = "WHDN-DT",
        resolution = "1080i",
        audio = "AC3 5.1",
        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        liveEventTitle = "Justice for All With Judge Cristin...",
        scoreBug = "2:00 PM - 2:30 PM"
    ),
    TabloChannel(
        id = "ch_gltv_9_4",
        major = 9,
        minor = 4,
        network = "GLTV",
        callSign = "WHDN-D4",
        resolution = "720p",
        audio = "Stereo",
        streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        liveEventTitle = "Suits",
        scoreBug = "3:00 PM - 4:00 PM"
    ),
    TabloChannel(
        id = "ch_cbs_11_1",
        major = 11,
        minor = 1,
        network = "CBS",
        callSign = "WINK-DT",
        resolution = "1080i",
        audio = "AC3 5.1",
        streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        liveEventTitle = "Beyond the Gates",
        scoreBug = "2:00 PM - 3:00 PM"
    ),
    TabloChannel(
        id = "ch_mnt_11_2",
        major = 11,
        minor = 2,
        network = "my network TV",
        callSign = "WINK-D2",
        resolution = "720p",
        audio = "AC3 5.1",
        streamUrl = "https://test-streams.mux.dev/test_001/stream.m3u8",
        liveEventTitle = "Alice",
        scoreBug = "2:00 PM - 2:30 PM"
    ),
    TabloChannel(
        id = "ch_ind_11_3",
        major = 11,
        minor = 3,
        network = "INDEPENDENT",
        callSign = "WINK-D3",
        resolution = "720p",
        audio = "Stereo",
        streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        liveEventTitle = "WINK-TV 24 Hour Live Local Weather",
        scoreBug = "2:00 PM - 6:00 PM"
    ),
    TabloChannel(
        id = "ch_ind_16_1",
        major = 16,
        minor = 1,
        network = "INDEPENDENT",
        callSign = "WWDT-DT",
        resolution = "1080i",
        audio = "AC3 5.1",
        streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
        liveEventTitle = "Court TV: Live Trial Coverage",
        scoreBug = "2:00 PM - 4:00 PM"
    ),
    TabloChannel(
        id = "ch_uni_18_1",
        major = 18,
        minor = 1,
        network = "Univision",
        callSign = "WLZE-LD",
        resolution = "1080i",
        audio = "AC3 5.1",
        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        liveEventTitle = "¡Siéntese quien pueda!",
        scoreBug = "2:00 PM - 3:00 PM"
    ),
    TabloChannel(
        id = "ch_unimas_18_2",
        major = 18,
        minor = 2,
        network = "UniMás",
        callSign = "WLZE-L2",
        resolution = "720p",
        audio = "Stereo",
        streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        liveEventTitle = "La mentira",
        scoreBug = "2:00 PM - 3:00 PM"
    ),
    // Streaming TV FAST Channels (Screenshots 1, 2)
    TabloChannel(
        id = "ch_scripps_500_1",
        major = 500,
        minor = 1,
        network = "Scripps News",
        callSign = "SCRIPPS",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        liveEventTitle = "Scripps News Live: National Report",
        scoreBug = "Live 24/7"
    ),
    TabloChannel(
        id = "ch_bloomberg_500_3",
        major = 500,
        minor = 3,
        network = "Bloomberg TV+",
        callSign = "BLOOMBERG",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        liveEventTitle = "Bloomberg Markets: Americas",
        scoreBug = "Wall Street Live"
    ),
    TabloChannel(
        id = "ch_bloomberg_500_4",
        major = 500,
        minor = 4,
        network = "Bloomberg Originals",
        callSign = "BLMB-ORIG",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        liveEventTitle = "The Circuit With Emily Chang",
        scoreBug = "Tech Documentary"
    ),
    TabloChannel(
        id = "ch_outside_501_1",
        major = 501,
        minor = 1,
        network = "Outside",
        callSign = "OUTSIDE",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://test-streams.mux.dev/test_001/stream.m3u8",
        liveEventTitle = "Extreme Mountain Expeditions",
        scoreBug = "Outdoor Adventure"
    ),
    TabloChannel(
        id = "ch_popsci_501_11",
        major = 501,
        minor = 11,
        network = "Popular Science",
        callSign = "POPSCI",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/big_buck_bunny.mp4",
        liveEventTitle = "Future Tech Innovations",
        scoreBug = "Science Showcase"
    ),
    TabloChannel(
        id = "ch_surf_501_12",
        major = 501,
        minor = 12,
        network = "Surf Cinema",
        callSign = "SURF",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
        liveEventTitle = "Big Wave Challenge: Tahiti Pro",
        scoreBug = "Action Sports"
    ),
    TabloChannel(
        id = "ch_msg_501_2",
        major = 501,
        minor = 2,
        network = "MSG SportsZone",
        callSign = "MSG-ZONE",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        liveEventTitle = "MSG Sports Recap & Highlights",
        scoreBug = "Sports Zone"
    ),
    TabloChannel(
        id = "ch_wsn_501_22",
        major = 501,
        minor = 22,
        network = "Women's Sports Network",
        callSign = "WSN",
        resolution = "1080p",
        audio = "Stereo",
        streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
        liveEventTitle = "WNBA Game of the Week",
        scoreBug = "Live Sports"
    )
)

fun defaultAiringsMap(): Map<String, TabloAiring> = mapOf(
    "ch_ind_9_1" to TabloAiring(airingId = "air_1", channelId = "ch_ind_9_1", title = "Justice for All With Judge Cristin...", description = "Judge Cristina Perez presides over small-claims courtroom disputes.", durationSeconds = 1800),
    "ch_gltv_9_4" to TabloAiring(airingId = "air_2", channelId = "ch_gltv_9_4", title = "Suits", description = "Harvey Specter and Mike Ross navigate high-stakes corporate legal battles.", durationSeconds = 3600),
    "ch_cbs_11_1" to TabloAiring(airingId = "air_3", channelId = "ch_cbs_11_1", title = "Beyond the Gates", description = "Drama series exploring the prestigious and secretive private gated community.", durationSeconds = 3600),
    "ch_mnt_11_2" to TabloAiring(airingId = "air_4", channelId = "ch_mnt_11_2", title = "Alice", description = "Classic comedy series following Alice Hyatt working at Mel's Diner.", durationSeconds = 1800),
    "ch_ind_11_3" to TabloAiring(airingId = "air_5", channelId = "ch_ind_11_3", title = "WINK-TV 24 Hour Live Local Weather", description = "Continuous local radar, Doppler velocity, and tropical track reports.", durationSeconds = 14400),
    "ch_ind_16_1" to TabloAiring(airingId = "air_6", channelId = "ch_ind_16_1", title = "Court TV: Live Trial Coverage", description = "Gavel-to-gavel live trial coverage and legal analysis from top experts.", durationSeconds = 7200),
    "ch_uni_18_1" to TabloAiring(airingId = "air_7", channelId = "ch_uni_18_1", title = "¡Siéntese quien pueda!", description = "En vivo: Noticias del espectáculo y debates con panel de celebridades.", durationSeconds = 3600),
    "ch_unimas_18_2" to TabloAiring(airingId = "air_8", channelId = "ch_unimas_18_2", title = "La mentira", description = "Telenovela dramática sobre secretos familiares, amor y redención.", durationSeconds = 3600),
    "ch_scripps_500_1" to TabloAiring(airingId = "air_9", channelId = "ch_scripps_500_1", title = "Scripps News Live: National Report", description = "In-depth investigative reports and unbiased news from around the country.", durationSeconds = 3600),
    "ch_bloomberg_500_3" to TabloAiring(airingId = "air_10", channelId = "ch_bloomberg_500_3", title = "Bloomberg Markets: Americas", description = "Market close commentary, earnings reports, and global financial analysis.", durationSeconds = 3600),
    "ch_outside_501_1" to TabloAiring(airingId = "air_11", channelId = "ch_outside_501_1", title = "Extreme Mountain Expeditions", description = "World-class mountaineers tackle alpine ascents across the Rockies.", durationSeconds = 3600),
    "ch_msg_501_2" to TabloAiring(airingId = "air_12", channelId = "ch_msg_501_2", title = "MSG Sports Recap & Highlights", description = "Highlights, scores, analysis, and interviews from around the leagues.", durationSeconds = 3600)
)
