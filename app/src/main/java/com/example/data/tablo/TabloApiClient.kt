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
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Local REST API client communicating directly with Tablo Gen 4 DVR over LAN,
 * secured using HMAC-MD5 request signing.
 */
class TabloApiClient(
    val authService: TabloAuthService = TabloAuthService()
) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fastHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val formMediaType = "application/x-www-form-urlencoded".toMediaType()

    /**
     * Unauthenticated ping to verify physical reachability and extract device SID.
     * GET http://$host:$port/ping
     */
    suspend fun ping(host: String, port: Int = 8885): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$host:$port/ping"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                    .build()

                fastHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Ping failed with HTTP ${response.code}")
                        )
                    }
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val sid = json.optString("sid", "")
                    Result.success(sid)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Query /server/info with HMAC-MD5 signing to retrieve device specifications.
     */
    suspend fun getServerInfo(host: String, port: Int = 8885, fast: Boolean = false): Result<TabloDevice> =
        withContext(Dispatchers.IO) {
            try {
                val client = if (fast) fastHttpClient else httpClient
                val path = "/server/info"
                val url = "http://$host:$port$path"

                val (authHeader, dateHeader) = authService.makeDeviceAuth("GET", path)

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", authHeader)
                    .header("Date", dateHeader)
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        return@withContext Result.failure(
                            Exception("Tablo device rejected request (401 Unauthorized). Account login is required for Tablo Gen 4.")
                        )
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("HTTP error ${response.code}: ${response.message}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(Exception("Empty response from /server/info"))

                    val json = JSONObject(body)

                    val serverId = json.optString("server_id", "")
                    val name = json.optString("name", "Tablo Gen 4").ifBlank { "Tablo Gen 4" }
                    val timezone = json.optString("timezone", "")
                    val version = json.optString("version", "")

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
     * Retrieve channel guide. If cloud tokens are provided, queries Tablo Cloud Guide
     * (the primary guide source on Gen 4). Falls back to local device signed guide query.
     */
    suspend fun getChannels(
        host: String,
        port: Int = 8885,
        accessToken: String? = null,
        lighthouseToken: String? = null
    ): Result<List<TabloChannel>> = withContext(Dispatchers.IO) {
        // Priority 1: Cloud Guide if tokens available
        if (!accessToken.isNullOrBlank() && !lighthouseToken.isNullOrBlank()) {
            val cloudRes = authService.getCloudChannels(accessToken, lighthouseToken)
            if (cloudRes.isSuccess) {
                return@withContext cloudRes
            }
        }

        // Priority 2: Direct local device query with HMAC signature
        try {
            val path = "/guide/channels"
            val url = "http://$host:$port$path"
            val (authHeader, dateHeader) = authService.makeDeviceAuth("GET", path)

            val listRequest = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", authHeader)
                .header("Date", dateHeader)
                .header("Accept", "application/json")
                .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                .build()

            httpClient.newCall(listRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to retrieve channels: HTTP ${response.code}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty channels response"))

                val jsonArray = JSONArray(body)
                val channels = mutableListOf<TabloChannel>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.get(i)
                    if (item is JSONObject) {
                        val ch = parseChannelJson(item)
                        if (ch != null) channels.add(ch)
                    } else if (item is String || item is Number) {
                        val pathOrId = item.toString()
                        val chId = pathOrId.substringAfterLast("/")
                        channels.add(
                            TabloChannel(
                                id = chId,
                                major = chId.toIntOrNull() ?: (i + 1),
                                minor = 1,
                                network = "Channel $chId",
                                callSign = "CH $chId"
                            )
                        )
                    }
                }

                Result.success(channels.sortedWith(compareBy({ it.major }, { it.minor })))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseChannelJson(json: JSONObject, fallbackPath: String = ""): TabloChannel? {
        try {
            val channelObj = if (json.has("channel")) json.getJSONObject("channel") else json
            val objectId = json.optString("object_id", "")
                .ifBlank {
                    json.optString("identifier", "")
                }
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
            val resolution = channelObj.optString("resolution", if (major > 0) "1080i" else "720p")
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
                val cleanId = channelId.substringAfterLast("/")
                val path = "/guide/channels/$cleanId/airings"
                val url = "http://$host:$port$path"
                val (authHeader, dateHeader) = authService.makeDeviceAuth("GET", path)

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", authHeader)
                    .header("Date", dateHeader)
                    .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                    .build()

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
                            val airing = parseAiringJson(item, cleanId)
                            if (airing != null) airings.add(airing)
                        }
                    }
                }
                Result.success(airings)
            } catch (e: Exception) {
                Result.failure(e)
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
     * Start watching a live channel on Tablo Gen 4 over LAN.
     * Uses HMAC-MD5 signing and required device payload.
     * Returns TabloStream with the HLS playlist_url.
     */
    suspend fun watchChannel(
        host: String,
        channelId: String,
        clientId: String,
        port: Int = 8885
    ): Result<TabloStream> = withContext(Dispatchers.IO) {
        try {
            val cleanChannelId = channelId.substringAfterLast("/")
            val path = "/guide/channels/$cleanChannelId/watch"
            val effectiveClientId = if (clientId.isNotBlank()) clientId else UUID.randomUUID().toString()

            // Construct payload matching Tablo 4th Gen iOS client specification
            val payloadJson = JSONObject().apply {
                put("bandwidth", JSONObject.NULL)
                val extraObj = JSONObject().apply {
                    put("limitedAdTracking", 1)
                    put("deviceOSVersion", "16.6")
                    put("lang", "en_US")
                    put("height", 1080)
                    put("deviceId", "00000000-0000-0000-0000-000000000000")
                    put("width", 1920)
                    put("deviceModel", "iPhone10,1")
                    put("deviceMake", "Apple")
                    put("deviceOS", "iOS")
                }
                put("extra", extraObj)
                put("device_id", effectiveClientId)
                put("platform", "ios")
            }

            val bodyString = payloadJson.toString()

            // CRITICAL Tablo Gen 4 LighthouseTV routing rule:
            // The request URL must contain "?lh" to route to the Gen 4 LighthouseTV service.
            // The HMAC-MD5 signature is computed on the path WITHOUT "?lh".
            val (authHeader, dateHeader) = authService.makeDeviceAuth("POST", path, bodyString)

            var lastResponseCode = 0
            var lastResponseBody = ""

            // Strategy 1: Tablo Gen 4 with ?lh and application/x-www-form-urlencoded
            try {
                val primaryUrl = "http://$host:$port$path?lh"
                val primaryRequest = Request.Builder()
                    .url(primaryUrl)
                    .post(bodyString.toRequestBody(formMediaType))
                    .header("Authorization", authHeader)
                    .header("Date", dateHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                    .build()

                httpClient.newCall(primaryRequest).execute().use { response ->
                    lastResponseCode = response.code
                    lastResponseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        return@withContext parseWatchResponse(lastResponseBody, cleanChannelId, host)
                    }
                }
            } catch (e: Exception) {
                lastResponseBody = e.message ?: "Primary request error"
            }

            // Strategy 2: Tablo Gen 4 with ?lh and application/json
            if (lastResponseCode == 404 || lastResponseCode == 400 || lastResponseCode == 0) {
                try {
                    val jsonUrl = "http://$host:$port$path?lh"
                    val jsonRequest = Request.Builder()
                        .url(jsonUrl)
                        .post(bodyString.toRequestBody(jsonMediaType))
                        .header("Authorization", authHeader)
                        .header("Date", dateHeader)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                        .build()

                    httpClient.newCall(jsonRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            return@withContext parseWatchResponse(body, cleanChannelId, host)
                        }
                    }
                } catch (_: Exception) {}
            }

            // Strategy 3: Tablo without ?lh
            if (lastResponseCode == 404 || lastResponseCode == 0) {
                try {
                    val directUrl = "http://$host:$port$path"
                    val directRequest = Request.Builder()
                        .url(directUrl)
                        .post(bodyString.toRequestBody(formMediaType))
                        .header("Authorization", authHeader)
                        .header("Date", dateHeader)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                        .build()

                    httpClient.newCall(directRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            return@withContext parseWatchResponse(body, cleanChannelId, host)
                        }
                    }
                } catch (_: Exception) {}
            }

            // Strategy 4: Legacy Tablo with blank POST
            if (lastResponseCode == 404 || lastResponseCode == 0) {
                try {
                    val blankUrl = "http://$host:$port$path"
                    val (blankAuth, blankDate) = authService.makeDeviceAuth("POST", path, "")
                    val blankRequest = Request.Builder()
                        .url(blankUrl)
                        .post("".toRequestBody(formMediaType))
                        .header("Authorization", blankAuth)
                        .header("Date", blankDate)
                        .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                        .build()

                    httpClient.newCall(blankRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            return@withContext parseWatchResponse(body, cleanChannelId, host)
                        }
                    }
                } catch (_: Exception) {}
            }

            if (lastResponseCode == 401) {
                return@withContext Result.failure(
                    Exception("Device rejected watch request (401). Please verify you are signed into your Tablo account.")
                )
            }

            Result.failure(
                Exception("Tablo watch request failed with HTTP $lastResponseCode: $lastResponseBody")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseWatchResponse(body: String, cleanChannelId: String, host: String): Result<TabloStream> {
        return try {
            val json = JSONObject(body)
            var playlistUrl = json.optString("playlist_url", "")
            if (playlistUrl.isBlank()) {
                playlistUrl = json.optString("url", "")
            }

            val token = json.optString("token", null)
            val expires = json.optString("expires", null)

            if (playlistUrl.isBlank()) {
                return Result.failure(
                    Exception("Tablo did not provide playlist_url in watch response: $body")
                )
            }

            // Resolve relative URLs to Tablo host
            if (!playlistUrl.startsWith("http://") && !playlistUrl.startsWith("https://")) {
                val cleanPath = if (playlistUrl.startsWith("/")) playlistUrl else "/$playlistUrl"
                playlistUrl = "http://$host:8888$cleanPath"
            }

            Result.success(
                TabloStream(
                    channelId = cleanChannelId,
                    playlistUrl = playlistUrl,
                    token = token,
                    expires = expires
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop watching and release the tuner on the Tablo Gen 4 DVR.
     */
    suspend fun stopWatching(host: String, token: String?, port: Int = 8885) =
        withContext(Dispatchers.IO) {
            if (token.isNullOrBlank()) return@withContext
            try {
                val path = "/stream/stop"
                val url = "http://$host:$port$path"
                val bodyString = JSONObject().put("token", token).toString()
                val (authHeader, dateHeader) = authService.makeDeviceAuth("POST", path, bodyString)

                val request = Request.Builder()
                    .url(url)
                    .post(bodyString.toRequestBody(jsonMediaType))
                    .header("Authorization", authHeader)
                    .header("Date", dateHeader)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", TabloAuthService.LOCAL_USER_AGENT)
                    .build()

                httpClient.newCall(request).execute().close()
            } catch (_: Exception) {}
        }
}
