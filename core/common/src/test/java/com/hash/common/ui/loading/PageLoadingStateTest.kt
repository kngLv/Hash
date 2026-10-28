package com.hash.common.ui.loading

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageLoadingStateTest {

    @Test
    fun show_makes_loading_visible() {
        val state = PageLoadingState()
        state.show()
        assertTrue(state.shouldShowLoading.value)
    }

    @Test
    fun dismiss_hides_loading() {
        val state = PageLoadingState()
        state.show()
        state.dismiss()
        assertFalse(state.shouldShowLoading.value)
    }

    @Test
    fun repeated_show_and_dismiss_are_idempotent() {
        val state = PageLoadingState()
        state.show()
        state.show()
        assertTrue(state.shouldShowLoading.value)
        state.dismiss()
        state.dismiss()
        assertFalse(state.shouldShowLoading.value)
    }
}
