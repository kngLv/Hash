package com.hash.common.ui.toolbar

import org.junit.Assert.assertEquals
import org.junit.Test

class HashToolbarTitleLayoutTest {

    @Test
    fun `center title reserves equal space for the wider action area`() {
        val bounds = HashToolbar.calculateTitleBounds(
            parentWidth = 360,
            titleWidth = 160,
            startAreaWidth = 48,
            endAreaWidth = 96,
            gravity = HashToolbar.TitleGravity.CENTER,
        )

        assertEquals(100, bounds.left)
        assertEquals(260, bounds.right)
    }

    @Test
    fun `start and end titles avoid their adjacent action area`() {
        val startBounds = HashToolbar.calculateTitleBounds(
            parentWidth = 360,
            titleWidth = 160,
            startAreaWidth = 48,
            endAreaWidth = 96,
            gravity = HashToolbar.TitleGravity.START,
        )
        val endBounds = HashToolbar.calculateTitleBounds(
            parentWidth = 360,
            titleWidth = 160,
            startAreaWidth = 48,
            endAreaWidth = 96,
            gravity = HashToolbar.TitleGravity.END,
        )

        assertEquals(48, startBounds.left)
        assertEquals(208, startBounds.right)
        assertEquals(104, endBounds.left)
        assertEquals(264, endBounds.right)
    }

    @Test
    fun `start and end titles reserve the configured edge padding`() {
        val startBounds = HashToolbar.calculateTitleBounds(
            parentWidth = 360,
            titleWidth = 160,
            startAreaWidth = 48,
            endAreaWidth = 96,
            gravity = HashToolbar.TitleGravity.START,
            edgePadding = 12,
        )
        val endBounds = HashToolbar.calculateTitleBounds(
            parentWidth = 360,
            titleWidth = 160,
            startAreaWidth = 48,
            endAreaWidth = 96,
            gravity = HashToolbar.TitleGravity.END,
            edgePadding = 12,
        )

        assertEquals(60, startBounds.left)
        assertEquals(220, startBounds.right)
        assertEquals(92, endBounds.left)
        assertEquals(252, endBounds.right)
    }
}
