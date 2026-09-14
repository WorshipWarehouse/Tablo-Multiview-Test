package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MultiviewLayoutMode
import com.example.model.TabloDevice
import org.json.JSONArray

/**
 * Local key-value store for registered Tablo device and user session preferences.
 * Stored locally on Fire TV without any cloud dependency.
 */
class TabloPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tablo_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_ID = "server_id"
        private const val KEY_NAME = "device_name"
        private const val KEY_HOST = "device_host"
        private const val KEY_PORT = "device_port"
        private const val KEY_MODEL = "device_model"
        private const val KEY_VERSION = "device_version"
        private const val KEY_TIMEZONE = "device_timezone"
        private const val KEY_TUNERS = "device_tuners"
        private const val KEY_LAST_CONNECTED = "last_connected"

        private const val KEY_LAST_LAYOUT_MODE = "last_layout_mode"
        private const val KEY_LAST_PANE_CHANNELS = "last_pane_channels"
        private const val KEY_LAST_ACTIVE_PANE = "last_active_pane"

        private const val KEY_AUTH_EMAIL = "auth_email"
        private const val KEY_AUTH_PASSWORD = "auth_password"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_LIGHTHOUSE_TOKEN = "lighthouse_token"
        private const val KEY_PROFILE_ID = "profile_id"
        private const val KEY_CLIENT_ID = "client_id"
    }

    fun getClientId(): String {
        var cid = prefs.getString(KEY_CLIENT_ID, null)
        if (cid.isNullOrBlank()) {
            cid = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_CLIENT_ID, cid).apply()
        }
        return cid
    }

    fun saveAuthSession(
        email: String,
        password: String,
        accessToken: String,
        profileId: String,
        lighthouseToken: String? = null
    ) {
        val editor = prefs.edit()
            .putString(KEY_AUTH_EMAIL, email)
            .putString(KEY_AUTH_PASSWORD, password)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_PROFILE_ID, profileId)
        if (!lighthouseToken.isNullOrBlank()) {
            editor.putString(KEY_LIGHTHOUSE_TOKEN, lighthouseToken)
        }
        editor.apply()
    }

    fun saveLighthouseToken(token: String) {
        prefs.edit().putString(KEY_LIGHTHOUSE_TOKEN, token).apply()
    }

    fun getAuthEmail(): String? = prefs.getString(KEY_AUTH_EMAIL, null)
    fun getAuthPassword(): String? = prefs.getString(KEY_AUTH_PASSWORD, null)
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getLighthouseToken(): String? = prefs.getString(KEY_LIGHTHOUSE_TOKEN, null)
    fun getProfileId(): String? = prefs.getString(KEY_PROFILE_ID, null)

    fun clearAuthSession() {
        prefs.edit()
            .remove(KEY_AUTH_EMAIL)
            .remove(KEY_AUTH_PASSWORD)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_LIGHTHOUSE_TOKEN)
            .remove(KEY_PROFILE_ID)
            .apply()
    }

    fun saveRegisteredDevice(device: TabloDevice) {
        prefs.edit()
            .putString(KEY_SERVER_ID, device.serverId)
            .putString(KEY_NAME, device.name)
            .putString(KEY_HOST, device.host)
            .putInt(KEY_PORT, device.port)
            .putString(KEY_MODEL, device.modelName)
            .putString(KEY_VERSION, device.version)
            .putString(KEY_TIMEZONE, device.timezone)
            .putInt(KEY_TUNERS, device.tunerCount)
            .putLong(KEY_LAST_CONNECTED, device.lastConnected)
            .apply()
    }

    fun getRegisteredDevice(): TabloDevice? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val serverId = prefs.getString(KEY_SERVER_ID, "") ?: ""
        val name = prefs.getString(KEY_NAME, "Tablo Gen 4") ?: "Tablo Gen 4"
        val port = prefs.getInt(KEY_PORT, 8885)
        val model = prefs.getString(KEY_MODEL, "Tablo Gen 4") ?: "Tablo Gen 4"
        val version = prefs.getString(KEY_VERSION, "") ?: ""
        val timezone = prefs.getString(KEY_TIMEZONE, "") ?: ""
        val tuners = prefs.getInt(KEY_TUNERS, 2)
        val lastConnected = prefs.getLong(KEY_LAST_CONNECTED, 0L)

        return TabloDevice(
            serverId = serverId,
            name = name,
            host = host,
            port = port,
            modelName = model,
            version = version,
            timezone = timezone,
            tunerCount = tuners,
            lastConnected = lastConnected
        )
    }

    fun clearRegisteredDevice() {
        prefs.edit()
            .remove(KEY_SERVER_ID)
            .remove(KEY_NAME)
            .remove(KEY_HOST)
            .remove(KEY_PORT)
            .remove(KEY_MODEL)
            .remove(KEY_VERSION)
            .remove(KEY_TIMEZONE)
            .remove(KEY_TUNERS)
            .remove(KEY_LAST_CONNECTED)
            .apply()
    }

    fun saveLastLayout(mode: MultiviewLayoutMode, paneChannelIds: List<String?>, activePane: Int) {
        val array = JSONArray()
        paneChannelIds.forEach { array.put(it ?: "") }

        prefs.edit()
            .putString(KEY_LAST_LAYOUT_MODE, mode.name)
            .putString(KEY_LAST_PANE_CHANNELS, array.toString())
            .putInt(KEY_LAST_ACTIVE_PANE, activePane)
            .apply()
    }

    fun saveLastLayoutMode(mode: MultiviewLayoutMode) {
        prefs.edit().putString(KEY_LAST_LAYOUT_MODE, mode.name).apply()
    }

    fun getLastLayoutMode(): MultiviewLayoutMode {
        val modeStr = prefs.getString(KEY_LAST_LAYOUT_MODE, null) ?: return MultiviewLayoutMode.ONE_PANE
        return try {
            MultiviewLayoutMode.valueOf(modeStr)
        } catch (_: Exception) {
            MultiviewLayoutMode.ONE_PANE
        }
    }

    fun getLastPaneChannels(): List<String> {
        val jsonStr = prefs.getString(KEY_LAST_PANE_CHANNELS, null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun getLastActivePane(): Int = prefs.getInt(KEY_LAST_ACTIVE_PANE, 0)
}
