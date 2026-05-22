package com.hereliesaz.cuedetat.domain

import android.graphics.PointF
import com.google.gson.Gson
import com.hereliesaz.cuedetat.view.state.TableSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableScanModelSerializationTest {

    private val gson = Gson()

    @Test
    fun newFieldsSurviveRoundTrip() {
        val histograms = mapOf(
            PocketId.TL to listOf(0.1f, 0.2f, 0.3f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            PocketId.TR to listOf(0.05f, 0.1f, 0.15f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )
        val model = TableScanModel(
            pockets = emptyList(),
            lensWarpTps = TpsWarpData(listOf(PointF(0f, 0f)), listOf(PointF(0f, 0f))),
            tableSize = TableSize.EIGHT_FT,
            feltColorHsv = listOf(120f, 0.5f, 0.4f),
            scanLatitude = null,
            scanLongitude = null,
            pocketSurroundHistograms = histograms,
            calibrationTimestamp = 1_700_000_000_000L
        )
        val json = gson.toJson(model)
        val decoded = gson.fromJson(json, TableScanModel::class.java)
        assertEquals(2, decoded.pocketSurroundHistograms?.size)
        assertEquals(1_700_000_000_000L, decoded.calibrationTimestamp)
    }

    @Test
    fun oldModelWithoutNewFieldsDeserializesToDefaults() {
        val oldJson = """{"pockets":[],"lensWarpTps":{"srcPoints":[],"dstPoints":[]},"tableSize":"EIGHT_FT","feltColorHsv":[120.0,0.5,0.4]}"""
        val decoded = gson.fromJson(oldJson, TableScanModel::class.java)
        // Gson bypasses Kotlin default params; null means "no histogram data" → pass-by-default in wrong-table gate
        assertTrue(decoded.pocketSurroundHistograms.isNullOrEmpty())
        assertEquals(0L, decoded.calibrationTimestamp)
    }
}
