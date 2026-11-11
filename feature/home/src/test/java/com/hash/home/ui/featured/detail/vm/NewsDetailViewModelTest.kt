package com.hash.home.ui.featured.detail.vm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hash.home.data.model.NewsDetailBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewsDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 相同新闻在请求中或已加载时不重复请求() = runTest {
        var loadCount = 0
        val viewModel = NewsDetailViewModel {
            loadCount++
            NewsDetailBean()
        }

        viewModel.load("news-1")
        viewModel.load("news-1")
        advanceUntilIdle()
        viewModel.load("news-1")

        assertEquals(1, loadCount)
    }
}
