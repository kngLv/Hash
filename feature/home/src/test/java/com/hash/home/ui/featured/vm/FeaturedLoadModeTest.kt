package com.hash.home.ui.featured.vm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedLoadModeTest {

    @Test
    fun `refresh bypasses cache and replaces the list`() {
        assertTrue(FeaturedLoadMode.REFRESH.bypassCache)
        assertFalse(FeaturedLoadMode.REFRESH.append)
    }

    @Test
    fun `next page uses cache and appends to the list`() {
        assertFalse(FeaturedLoadMode.NEXT_PAGE.bypassCache)
        assertTrue(FeaturedLoadMode.NEXT_PAGE.append)
    }
}
