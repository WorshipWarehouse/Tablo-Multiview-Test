package com.example.data.tablo

import com.example.model.TabloChannel
import com.example.model.TabloDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service managing Tablo Gen 4 (LighthouseTV) Cloud Authentication and HMAC-MD5 local request signing.
 *
 * Tablo Gen 4 requires cloud authentication with the user's Tablo account (email/password)
 * to retrieve linked devices, obtain the lighthouse session token, and authorize local streaming.
 */
class TabloAuthService {

    companion object {
        const val CLOUD_HOST = "https://lighthousetv.ewscloud.com"
        const val CLOUD_USER_AGENT = "Tablo-FAST/2.0.0 (Mobile; iPhone; iOS 16.6)"
        const val LOCAL_USER_AGENT = "Tablo-FAST/1.7.0 (Mobile; iPhone; iOS 18.4)"

        // HMAC-MD5 signing keys extracted from official Tablo client
        private const val HASH_KEY = "6l8jU5N43cEilqItmT3U2M2PFM3qPziilXqau9ys"
        private const val DEVICE_KEY = "ljpg6ZkwShVv8aI12E2LP55Ep8vq1uYDPvX0DdTB"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class CloudLoginResult(
        val accessToken: String,
        val tokenType: String
    )

    data class CloudAccountResult(
        val profileId: String,
        val profileName: String,
        val devices: List<TabloDevice>
    )

    /**
     * Authenticate with Tablo Cloud using user's email and password.
     * POST https://lighthousetv.ewscloud.com/api/v2/login/
     */
    suspend fun login(email: String, password: String): Result<CloudLoginResult> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$CLOUD_HOST/api/v2/login/"
                val jsonBody = JSONObject().apply {
                    put("email", email.trim())
                    put("password", password)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .header("User-Agent", CLOUD_USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Tablo login failed (HTTP ${response.code}): ${parseErrorMessage(body)}")
                        )
                    }

                    val json = JSONObject(body)
                    val accessToken = json.getString("access_token")
                    val tokenType = json.optString("token_type", "Bearer")
                    Result.success(CloudLoginResult(accessToken, tokenType))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Retrieve account profile and registered Tablo devices.
     * GET https://lighthousetv.ewscloud.com/api/v2/account/
     */
    suspend fun getAccountDevices(accessToken: String): Result<CloudAccountResult> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$CLOUD_HOST/api/v2/account/"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", "Bearer $accessToken")
                    .header("User-Agent", CLOUD_USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Failed to load Tablo account devices (HTTP ${response.code})")
                        )
                    }

                    val json = JSONObject(body)
                    val profilesArray = json.optJSONArray("profiles")
                    var profileId = ""
                    var profileName = ""
                    if (profilesArray != null && profilesArray.length() > 0) {
                        val firstProfile = profilesArray.getJSONObject(0)
                        profileId = firstProfile.optString("identifier", "")
                        profileName = firstProfile.optString("name", "Default")
                    }

                    val devicesArray = json.optJSONArray("devices") ?: JSONArray()
                    val devices = mutableListOf<TabloDevice>()

                    for (i in 0 until devicesArray.length()) {
                        val devJson = devicesArray.getJSONObject(i)
                        val name = devJson.optString("name", "Tablo Gen 4")
                        val serverId = devJson.optString("serverId", "")
                        val rawUrl = devJson.optString("url", "")

                        // Parse local IP and port from url (e.g. http://192.168.1.120:8885)
                        var host = "192.168.1.100"
                        var port = 8885
                        if (rawUrl.isNotBlank()) {
                            try {
                                val uri = URI(rawUrl)
                                host = uri.host ?: host
                                port = if (uri.port > 0) uri.port else 8885
                            } catch (_: Exception) {
                                val clean = rawUrl.removePrefix("http://").removePrefix("https://")
                                val parts = clean.split(":")
                                host = parts[0].trim()
                                if (parts.size > 1) {
                                    port = parts[1].toIntOrNull() ?: 8885
                                }
                            }
                        }

                        devices.add(
                            TabloDevice(
                                serverId = serverId,
                                name = name,
                                host = host,
                                port = port,
                                modelName = "Tablo Gen 4",
                                tunerCount = 2,
                                isWifi = true,
                                lastConnected = System.currentTimeMillis()
                            )
                        )
                    }

                    Result.success(CloudAccountResult(profileId, profileName, devices))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Select device to acquire a device-scoped Lighthouse token.
     * POST https://lighthousetv.ewscloud.com/api/v2/account/select/
     */
    suspend fun selectDevice(accessToken: String, profileId: String, serverId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$CLOUD_HOST/api/v2/account/select/"
                val jsonBody = JSONObject().apply {
                    put("pid", profileId)
                    put("sid", serverId)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .header("Authorization", "Bearer $accessToken")
                    .header("User-Agent", CLOUD_USER_AGENT)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Device selection failed (HTTP ${response.code})")
                        )
                    }

                    val json = JSONObject(body)
                    val token = json.getString("token")
                    Result.success(token)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetch channel guide from Tablo cloud for the selected device.
     * GET https://lighthousetv.ewscloud.com/api/v2/account/{lighthouseToken}/guide/channels/
     */
    suspend fun getCloudChannels(
        accessToken: String,
        lighthouseToken: String,
        includeOtt: Boolean = true
    ): Result<List<TabloChannel>> = withContext(Dispatchers.IO) {
        try {
            val path = "/api/v2/account/$lighthouseToken/guide/channels/"
            val url = "$CLOUD_HOST$path"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $accessToken")
                .header("Lighthouse", lighthouseToken)
                .header("Accept", "*/*")
                .header("User-Agent", CLOUD_USER_AGENT)
                .header("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to fetch cloud channel guide (HTTP ${response.code})")
                    )
                }

                val jsonArray = JSONArray(body)
                val channels = mutableListOf<TabloChannel>()

                for (i in 0 until jsonArray.length()) {
                    val raw = jsonArray.getJSONObject(i)
                    val kind = raw.optString("kind", "ota")
                    if (!includeOtt && kind == "ott") {
                        continue
                    }

                    val identifier = raw.getString("identifier")
                    val rawName = raw.optString("name", "")

                    val otaObj = raw.optJSONObject("ota")
                    val ottObj = raw.optJSONObject("ott")
                    val infoObj = otaObj ?: ottObj

                    val callSign = infoObj?.optString("callSign", null)
                        ?: rawName.ifBlank { identifier }
                    val network = infoObj?.optString("network", "") ?: ""
                    val major = infoObj?.optInt("major", 0) ?: 0
                    val minor = infoObj?.optInt("minor", 0) ?: 0

                    channels.add(
                        TabloChannel(
                            id = identifier,
                            major = major,
                            minor = minor,
                            network = network,
                            callSign = callSign,
                            resolution = if (major > 0) "1080i" else "720p",
                            channelPath = "/guide/channels/$identifier"
                        )
                    )
                }

                // Sort OTA channels first by number, then OTT channels alphabetically
                val sorted = channels.sortedWith { a, b ->
                    val aIsOta = a.major > 0
                    val bIsOta = b.major > 0
                    when {
                        aIsOta && !bIsOta -> -1
                        !aIsOta && bIsOta -> 1
                        aIsOta && bIsOta -> {
                            if (a.major != b.major) a.major.compareTo(b.major)
                            else a.minor.compareTo(b.minor)
                        }
                        else -> a.callSign.compareTo(b.callSign, ignoreCase = true)
                    }
                }

                Result.success(sorted)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate RFC 1123 GMT Date string required by Tablo local device auth.
     */
    fun deviceDate(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date())
    }

    /**
     * Compute HMAC-MD5 signing headers for local device requests.
     * Returns Pair(AuthorizationHeader, DateHeader).
     */
    fun makeDeviceAuth(method: String, path: String, body: String = ""): Pair<String, String> {
        val date = deviceDate()
        val msgHash = if (body.isNotEmpty()) md5Hex(body) else ""
        val payload = "$method\n$path\n$msgHash\n$date"
        val sig = hmacMd5Hex(HASH_KEY, payload)
        val authHeader = "tablo:$DEVICE_KEY:$sig"
        return Pair(authHeader, date)
    }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hmacMd5Hex(key: String, data: String): String {
        val mac = Mac.getInstance("HmacMD5")
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacMD5")
        mac.init(secretKeySpec)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            val json = JSONObject(body)
            json.optString("message", json.optString("detail", body.take(120)))
        } catch (_: Exception) {
            body.take(120)
        }
    }
}
