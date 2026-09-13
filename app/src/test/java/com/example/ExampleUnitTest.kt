package com.example

import com.example.model.MultiviewLayoutMode
import com.example.model.SavedLayout
import com.example.model.TabloAiring
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTabloChannelFormatting() {
        val channel1 = TabloChannel(
            id = "612675",
            major = 4,
            minor = 1,
            network = "NBC",
            callSign = "WNBC-HD",
            resolution = "1080i"
        )
        assertEquals("4.1", channel1.channelNumberFormatted)
        assertEquals("4.1  NBC", channel1.displayTitle)
        assertTrue(channel1.displaySubtitle.contains("WNBC-HD"))
        assertTrue(channel1.displaySubtitle.contains("1080i"))

        val channelMajorOnly = TabloChannel(
            id = "100",
            major = 7,
            minor = 0,
            network = "ABC",
            callSign = "WABC"
        )
        assertEquals("7", channelMajorOnly.channelNumberFormatted)
    }

    @Test
    fun testTabloDeviceBaseUrl() {
        val device = TabloDevice(
            serverId = "g4_test",
            name = "Den Tablo",
            host = "192.168.1.150",
            port = 8885,
            modelName = "Tablo Gen 4",
            tunerCount = 4
        )
        assertEquals("http://192.168.1.150:8885", device.baseUrl)
        assertTrue(device.displaySubtitle.contains("4 tuners"))
    }

    @Test
    fun testTabloAiringDuration() {
        val now = System.currentTimeMillis()
        val airing = TabloAiring(
            airingId = "air_123",
            channelId = "612675",
            title = "Nightly News",
            startTime = now - 60000L, // Started 1 minute ago
            durationSeconds = 1800 // 30 minutes
        )
        assertTrue(airing.isLiveNow)
        assertEquals(now - 60000L + 1800000L, airing.endTime)
    }

    @Test
    fun testSavedLayoutModel() {
        val layout = SavedLayout(
            id = 1L,
            name = "Sunday Football",
            mode = MultiviewLayoutMode.FOUR_PANE,
            channelIds = listOf("1", "2", "3", "4"),
            channelLabels = listOf("4.1 NBC", "2.1 CBS", "5.1 FOX", "7.1 ABC")
        )
        assertEquals(4, layout.mode.paneCount)
        assertEquals(4, layout.channelIds.size)
        assertEquals("Sunday Football", layout.name)
    }
}
