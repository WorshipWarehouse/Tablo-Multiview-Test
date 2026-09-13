package com.example.data.tablo

import com.example.model.TabloAiring
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import com.example.model.TabloStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Local REST API client communicating directly with Tablo Gen 4 DVR over LAN.
 * Reference: https://jessedp.github.io/tablo-api-docs/#tablo-api-introduction
 */
class TabloApiClient {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fastHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.MILLISECONDS)
        .readTimeout(1000, TimeUnit.MILLISECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Query /server/info to validate that a device is a physical Tablo and retrieve its specs.
     */
    suspend fun getServerInfo(host: String, port: Int = 8885, fast: Boolean = false): Result<TabloDevice> =
        withContext(Dispatchers.IO) {
            try {
                val client = if (fast) fastHttpClient else httpClient
                val url = "http://$host:$port/server/info"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("HTTP error ${response.code}: ${response.message}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response from /server/info"))

                    val json = JSONObject(body)

                    // Tablo /server/info fields
                    val serverId = json.optString("server_id", "")
                    val name = json.optString("name", "Tablo Gen 4").ifBlank { "Tablo Gen 4" }
                    val timezone = json.optString("timezone", "")
                    val version = json.optString("version", "")

                    // Model can be an object or string
                    var modelName = "Tablo Gen 4"
                    var tunerCount = 2
                    var isWifi = true
                    var modelType: String? = null

                    if (json.has("model")) {
                        val modelObj = json.optJSONObject("model")
                        if (modelObj != null) {
                            modelName = modelObj.optString("name", "Tablo Gen 4")
                            tunerCount = modelObj.optInt("tuners", 2)
                            isWifi = modelObj.optBoolean("wifi", true)
                            modelType = modelObj.optString("type", null)
                        } else {
                            modelName = json.optString("model", "Tablo Gen 4")
                        }
                    } else if (json.has("tuners")) {
                        tunerCount = json.optInt("tuners", 2)
                    }

                    val device = TabloDevice(
                        serverId = serverId,
                        name = name,
                        host = host,
                        port = port,
                        modelName = modelName,
                        modelType = modelType,
                        version = version,
                        timezone = timezone,
                        tunerCount = tunerCount,
                        isWifi = isWifi,
                        lastConnected = System.currentTimeMillis()
                    )

                    Result.success(device)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Retrieve all available live TV channels.
     * Tablo returns an array of paths from /guide/channels, then channel details.
     */
    suspend fun getChannels(host: String, port: Int = 8885): Result<List<TabloChannel>> =
        withContext(Dispatchers.IO) {
            try {
                val listUrl = "http://$host:$port/guide/channels"
                val listRequest = Request.Builder()
                    .url(listUrl)
                    .get()
                    .header("Accept", "application/json")
                    .build()

                val paths = mutableListOf<String>()
                httpClient.newCall(listRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Failed to retrieve channels: HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty channels response"))

                    val jsonArray = JSONArray(body)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.get(i)
                        if (item is String) {
                            paths.add(item)
                        } else if (item is JSONObject) {
                            // In some firmware versions, array of objects is returned directly
                            val ch = parseChannelJson(item)
                            if (ch != null) {
                                return@withContext Result.success(
                                    (0 until jsonArray.length()).mapNotNull { idx ->
                                        parseChannelJson(jsonArray.getJSONObject(idx))
                                    }.sortedWith(compareBy({ it.major }, { it.minor }))
                                )
                            }
                        }
                    }
                }

                if (paths.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // Batch fetch channels via POST /batch
                val batchChannels = fetchChannelsBatch(host, port, paths)
                if (batchChannels.isNotEmpty()) {
                    return@withContext Result.success(
                        batchChannels.sortedWith(compareBy({ it.major }, { it.minor }))
                    )
                }

                // Fallback to fetching individually if batch is not supported
                val channels = mutableListOf<TabloChannel>()
                for (path in paths) {
                    val ch = fetchChannelByPath(host, port, path)
                    if (ch != null) {
                        channels.add(ch)
                    }
                }

                Result.success(channels.sortedWith(compareBy({ it.major }, { it.minor })))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Attempt batch fetch for multiple channel paths using POST /batch.
     */
    private fun fetchChannelsBatch(host: String, port: Int, paths: List<String>): List<TabloChannel> {
        val channels = mutableListOf<TabloChannel>()
        try {
            val url = "http://$host:$port/batch"
            val requestBodyArray = JSONArray()
            paths.forEach { requestBodyArray.put(it) }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyArray.toString().toRequestBody(jsonMediaType))
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return emptyList()
                    val batchObj = JSONObject(body)
                    val keys = batchObj.keys()
                    while (keys.hasNext()) {
                        val pathKey = keys.next()
                        val channelJson = batchObj.optJSONObject(pathKey)
                        if (channelJson != null) {
                            val parsed = parseChannelJson(channelJson, pathKey)
                            if (parsed != null) {
                                channels.add(parsed)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return channels
    }

    private fun fetchChannelByPath(host: String, port: Int, path: String): TabloChannel? {
        return try {
            val cleanPath = if (path.startsWith("/")) path else "/$path"
            val url = "http://$host:$port$cleanPath"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    parseChannelJson(JSONObject(body), cleanPath)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseChannelJson(json: JSONObject, fallbackPath: String = ""): TabloChannel? {
        try {
            val channelObj = if (json.has("channel")) json.getJSONObject("channel") else json
            val objectId = json.optString("object_id", "")
                .ifBlank {
                    fallbackPath.substringAfterLast("/").ifBlank { "0" }
                }

            val major = channelObj.optInt("major", 0)
            val minor = channelObj.optInt("minor", 0)
            val network = channelObj.optString("network", "").ifBlank {
                channelObj.optString("name", "")
            }
            val callSign = channelObj.optString("call_sign", "").ifBlank {
                channelObj.optString("callSign", "")
            }
            val resolution = channelObj.optString("resolution", null)
            val audio = channelObj.optString("audio", null)
            val logoUrl = channelObj.optString("logo_url", null)

            return TabloChannel(
                id = objectId,
                major = major,
                minor = minor,
                network = network,
                callSign = callSign,
                resolution = resolution,
                audio = audio,
                logoUrl = logoUrl,
                channelPath = if (fallbackPath.isNotBlank()) fallbackPath else "/guide/channels/$objectId"
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Retrieve current airing for a channel.
     */
    suspend fun getChannelAirings(host: String, channelId: String, port: Int = 8885): Result<List<TabloAiring>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$host:$port/guide/channels/$channelId/airings"
                val request = Request.Builder().url(url).get().build()

                val airings = mutableListOf<TabloAiring>()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.success(emptyList())
                    }
                    val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                    val array = JSONArray(body)
                    for (i in 0 until minOf(array.length(), 10)) {
                        val item = array.get(i)
                        if (item is JSONObject) {
                            val airing = parseAiringJson(item, channelId)
                            if (airing != null) airings.add(airing)
                        } else if (item is String) {
                            // fetch single airing
                            val singleAiring = fetchAiringByPath(host, port, item, channelId)
                            if (singleAiring != null) airings.add(singleAiring)
                        }
                    }
                }
                Result.success(airings)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun fetchAiringByPath(host: String, port: Int, path: String, channelId: String): TabloAiring? {
        return try {
            val cleanPath = if (path.startsWith("/")) path else "/$path"
            val request = Request.Builder().url("http://$host:$port$cleanPath").get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    parseAiringJson(JSONObject(body), channelId)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAiringJson(json: JSONObject, channelId: String): TabloAiring? {
        return try {
            val objectId = json.optString("object_id", "")
            val showTitle = json.optString("show_title", "Live Broadcast")
            val airingDetails = json.optJSONObject("airing_details")
            var startTime = 0L
            var duration = 0

            if (airingDetails != null) {
                duration = airingDetails.optInt("duration", 0)
                val dtStr = airingDetails.optString("datetime", "")
                if (dtStr.isNotBlank()) {
                    startTime = parseIsoDateTime(dtStr)
                }
            }

            var episodeTitle: String? = null
            var description: String? = null
            val episodeObj = json.optJSONObject("episode")
            if (episodeObj != null) {
                episodeTitle = episodeObj.optString("title", null)
                description = episodeObj.optString("description", null)
            }

            TabloAiring(
                airingId = objectId,
                channelId = channelId,
                title = showTitle,
                episodeTitle = episodeTitle,
                description = description,
                startTime = startTime,
                durationSeconds = duration
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseIsoDateTime(isoString: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(isoString)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Start watching a live channel.
     * POST /guide/channels/{channel_id}/watch
     * Returns TabloStream with the HLS playlist_url.
     */
    suspend fun watchChannel(host: String, channelId: String, port: Int = 8885): Result<TabloStream> =
        withContext(Dispatchers.IO) {
            try {
                val cleanChannelId = channelId.substringAfterLast("/")
                val url = "http://$host:$port/guide/channels/$cleanChannelId/watch"
                val emptyBody = "{}".toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(emptyBody)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Tablo watch request failed with HTTP ${response.code}: ${response.message}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty watch response"))

                    val json = JSONObject(body)
                    var playlistUrl = json.optString("playlist_url", "")
                    val token = json.optString("token", null)
                    val expires = json.optString("expires", null)

                    if (playlistUrl.isBlank()) {
                        // Check if URL is under "stream" or "url"
                        playlistUrl = json.optString("url", "")
                    }

                    if (playlistUrl.isBlank()) {
                        return@withContext Result.failure(
                            Exception("Tablo did not provide a valid playlist_url in watch response: $body")
                        )
                    }

                    // Resolve relative URLs
                    if (!playlistUrl.startsWith("http://") && !playlistUrl.startsWith("https://")) {
                        val cleanPath = if (playlistUrl.startsWith("/")) playlistUrl else "/$playlistUrl"
                        playlistUrl = "http://$host:80$cleanPath"
                    }

                    Result.success(
                        TabloStream(
                            channelId = cleanChannelId,
                            playlistUrl = playlistUrl,
                            token = token,
                            expires = expires
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Send keepalive or stop watch signal to free hardware tuners.
     */
    suspend fun stopWatching(host: String, token: String?, port: Int = 8885) = withContext(Dispatchers.IO) {
        if (token.isNullOrBlank()) return@withContext
        try {
            val url = "http://$host:$port/stream/stop"
            val body = JSONObject().put("token", token).toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(body).build()
            httpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }
}
