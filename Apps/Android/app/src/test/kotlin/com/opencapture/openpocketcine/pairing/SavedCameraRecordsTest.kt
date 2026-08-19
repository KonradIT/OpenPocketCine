package com.opencapture.openpocketcine.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SavedCameraRecordsTest {
    @Test
    fun upsertKeepsCustomNameAndNewestFirst() {
        val first =
            SavedCamera("a", "OsmoPocket4P-1", "Osmo Pocket 4 Pro", "OPENPOCKETCINE", 1_000L, customName = "A-cam")
        val second = SavedCamera("b", "OsmoPocket3-2", "Osmo Pocket 3", null, 2_000L)
        val reconnect = SavedCamera("a", "OsmoPocket4P-1", "Osmo Pocket 4 Pro", "SSID2", 3_000L)
        val result = SavedCameras.upserting(reconnect, listOf(first, second))
        assertEquals(listOf("a", "b"), result.map { it.id })
        assertEquals("A-cam", result.first().customName)
        assertEquals("SSID2", result.first().lastSSID)
    }

    @Test
    fun emptyStoreLaunchesWizard() {
        assertTrue(SavedCameras.launchShowsWizard(emptyList()))
        assertTrue(!SavedCameras.launchShowsWizard(listOf(SavedCamera("a", "n", "m", null, 1))))
    }

    @Test
    fun roundTripJson() {
        val records =
            listOf(SavedCamera("id-1", "OsmoPocket4P-AAAA", "Osmo Pocket 4 Pro", "DJI-xxx", 42L, "Rig"))
        val decoded = SharedPreferencesSavedCameraStore.decode(SharedPreferencesSavedCameraStore.encode(records))
        assertEquals(records, decoded)
    }
}
