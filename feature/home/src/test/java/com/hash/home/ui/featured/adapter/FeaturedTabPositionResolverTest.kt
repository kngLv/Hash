package com.hash.home.ui.featured.adapter

import org.junit.Assert.assertEquals
import org.junit.Test

class FeaturedTabPositionResolverTest {

    @Test
    fun `keeps the selected tab when the server reorders tabs`() {
        val position = FeaturedTabPositionResolver.resolve(
            tabIds = listOf(3L, 1L, 2L),
            selectedTabId = 2L,
            fallbackPosition = 1,
        )

        assertEquals(2, position)
    }

    @Test
    fun `uses a valid fallback when the selected tab was removed`() {
        val position = FeaturedTabPositionResolver.resolve(
            tabIds = listOf(1L, 3L),
            selectedTabId = 2L,
            fallbackPosition = 2,
        )

        assertEquals(1, position)
    }
}
