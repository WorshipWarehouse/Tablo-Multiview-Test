package com.example.data.tablo

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.TabloDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

/**
 * Robust local network discovery for Tablo devices.
 * Implements:
 * 1. Documented UDP broadcast discovery on port 8881
 * 2. Documented Tablo assocserver cloud IP fallback
 * 3. Fast LAN subnet probe on port 8885 for networks that block UDP broadcasts
 * 4. Verification of each found candidate via GET /server/info
 */
class TabloDiscoveryService(
    private val context: Context,
    private val apiClient: TabloApiClient = TabloApiClient()
) {
    private val tag = "TabloDiscovery"

    private val cloudClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun discoverTablos(): List<TabloDevice> = withContext(Dispatchers.IO) {
        val discoveredIps = mutableSetOf<String>()

        // Acquire multicast lock if available (required on some Android TV builds for UDP)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("tablo_multicast_lock")?.apply {
            setReferenceCounted(true)
            try { acquire() } catch (_: Exception) {}
        }

        try {
            // Step 1: Run UDP Broadcast Discovery
            val udpIps = runUdpDiscovery()
            discoveredIps.addAll(udpIps)

            // Step 2: Query documented Assocserver for local Tablos on this WAN IP
            val assocIps = runAssocServerDiscovery()
            discoveredIps.addAll(assocIps)

            // Step 3: Fast Subnet Scan if no Tablos found or to find all LAN devices
            if (discoveredIps.isEmpty()) {
                val subnetIps = runFastSubnetProbe()
                discoveredIps.addAll(subnetIps)
            }
        } finally {
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }

        // Step 4: Validate each candidate IP via /server/info
        val validDevices = mutableListOf<TabloDevice>()
        coroutineScope {
            val deferredList = discoveredIps.map { ip ->
                async {
                    val result = apiClient.getServerInfo(ip, 8885)
                    result.getOrNull()
                }
            }
            val results = deferredList.awaitAll()
            results.filterNotNull().forEach { validDevices.add(it) }
        }

        validDevices.distinctBy { it.serverId.ifBlank { it.host } }
    }

    /**
     * Broadcasts UDP discovery packet to port 8881 and listens on 8882/random port.
     */
    private fun runUdpDiscovery(): Set<String> {
        val foundIps = mutableSetOf<String>()
        var socket: DatagramSocket? = null
        try {
            socket = try {
                DatagramSocket(8882).apply { reuseAddress = true }
            } catch (_: Exception) {
                DatagramSocket()
            }
            socket.broadcast = true
            socket.soTimeout = 2500

            val message = "Tablo".toByteArray(Charsets.UTF_8)

            // Broadcast destinations
            val destinations = mutableListOf<InetAddress>()
            try { destinations.add(InetAddress.getByName("255.255.255.255")) } catch (_: Exception) {}
            try { destinations.add(InetAddress.getByName("224.0.0.1")) } catch (_: Exception) {}

            // Add local subnet broadcast addresses
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val nif = interfaces.nextElement()
                    if (nif.isLoopback || !nif.isUp) continue
                    for (addr in nif.interfaceAddresses) {
                        val bcast = addr.broadcast
                        if (bcast != null) destinations.add(bcast)
                    }
                }
            } catch (_: Exception) {}

            // Send discovery packet to all targets
            for (dest in destinations) {
                try {
                    val packet = DatagramPacket(message, message.size, dest, 8881)
                    socket.send(packet)
                } catch (_: Exception) {}
            }

            // Receive responses
            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 2500) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val senderIp = receivePacket.address?.hostAddress
                    if (!senderIp.isNullOrBlank() && senderIp != "127.0.0.1") {
                        foundIps.add(senderIp)
                        val text = String(receivePacket.data, 0, receivePacket.length)
                        Log.d(tag, "UDP response from $senderIp: $text")
                    }
                } catch (_: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "UDP discovery error: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
        return foundIps
    }

    /**
     * Documented fallback discovery via Tablo's association server:
     * https://api.tablotv.com/assocserver/getipinfo/
     */
    private fun runAssocServerDiscovery(): Set<String> {
        val ips = mutableSetOf<String>()
        try {
            val url = "https://api.tablotv.com/assocserver/getipinfo/"
            val request = Request.Builder().url(url).get().build()
            cloudClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return emptySet()
                    val json = JSONObject(body)
                    val cpes = json.optJSONArray("cpes")
                    if (cpes != null) {
                        for (i in 0 until cpes.length()) {
                            val cpe = cpes.getJSONObject(i)
                            val privateIp = cpe.optString("private_ip", "")
                            if (privateIp.isNotBlank()) {
                                ips.add(privateIp)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "AssocServer discovery: ${e.message}")
        }
        return ips
    }

    /**
     * Subnet probe for environments where UDP broadcast is blocked by router isolation.
     * Probes ports 8885 on common IP ranges in the local subnet.
     */
    private suspend fun runFastSubnetProbe(): Set<String> = coroutineScope {
        val foundIps = mutableSetOf<String>()
        val localSubnet = getLocalSubnetPrefix() ?: return@coroutineScope emptySet()

        // Probe 1..254 concurrently in batches
        val candidates = (1..254).map { "$localSubnet.$it" }
        val deferredProbes = candidates.map { candidateIp ->
            async(Dispatchers.IO) {
                if (isPortOpen(candidateIp, 8885, 300)) {
                    candidateIp
                } else null
            }
        }
        val openIps = deferredProbes.awaitAll().filterNotNull()
        foundIps.addAll(openIps)
        foundIps
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalSubnetPrefix(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (nif.isLoopback || !nif.isUp) continue
                for (addr in nif.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val lastDot = host.lastIndexOf('.')
                            if (lastDot > 0) {
                                return host.substring(0, lastDot)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
