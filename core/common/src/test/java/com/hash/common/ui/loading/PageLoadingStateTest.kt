package com.hash.common.ui.loading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageLoadingStateTest {

    @Test
    fun show_publishes_default_loading_config() {
        val state = PageLoadingState()

        state.show()

        assertEquals(
            LoadingConfig(cancelOnTouchOutside = true),
            state.config.value,
        )
    }

    @Test
    fun dismiss_clears_loading_config() {
        val state = PageLoadingState()
        state.show()
        state.dismiss()

        assertNull(state.config.value)
    }

    @Test
    fun repeated_show_replaces_loading_config() {
        val state = PageLoadingState()
        state.show()
        state.show(delayMillis = 0L, cancelOnTouchOutside = true)

        assertEquals(
            LoadingConfig(delayMillis = 0L, cancelOnTouchOutside = true),
            state.config.value,
        )
    }

    @Test
    fun negative_delay_is_normalized_to_zero() {
        val state = PageLoadingState()

        state.show(delayMillis = -1L)

        assertEquals(LoadingConfig(delayMillis = 0L), state.config.value)
    }
}
