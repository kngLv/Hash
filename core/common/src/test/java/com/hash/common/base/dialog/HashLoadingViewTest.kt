package com.hash.common.base.dialog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashLoadingViewTest {

    @Test
    fun segment_intensity_flows_smoothly_between_hash_strokes() {
        assertEquals(1f, HashLoadingView.segmentIntensityAt(progress = 0f, segment = 0), 0.001f)
        assertEquals(0.04f, HashLoadingView.segmentIntensityAt(progress = 0f, segment = 2), 0.001f)
        assertTrue(
            HashLoadingView.segmentIntensityAt(progress = 0f, segment = 1) in 0.04f..1f,
        )
        assertEquals(1f, HashLoadingView.segmentIntensityAt(progress = 0.25f, segment = 1), 0.001f)
    }
}
