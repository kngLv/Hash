package com.hash.common.ui.insets

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemBarInsetsPolicyTest {

    @Test
    fun `ime inset wins when it is taller than navigation bar`() {
        assertEquals(320, SystemBarInsetsPolicy.resolveImeBottom(320, 96))
    }

    @Test
    fun `navigation inset wins when keyboard is hidden`() {
        assertEquals(96, SystemBarInsetsPolicy.resolveImeBottom(0, 96))
    }

    @Test
    fun `equal ime and navigation insets are preserved`() {
        assertEquals(96, SystemBarInsetsPolicy.resolveImeBottom(96, 96))
    }
}
