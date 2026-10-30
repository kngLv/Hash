package com.hash.home.data.repository

import android.app.Application
import com.hash.common.net.response.RollResponse
import com.hash.home.data.api.RollApi
import com.hash.home.data.model.NewsDetailBean
import com.hash.home.data.model.NewsListBean
import com.hash.home.data.model.NewsTypeListBean
import com.hash.net.LvHttp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NewsDetailRepositoryTest {

    @Before
    fun setUp() {
        LvHttp.Builder(Application())
            .setBaseUrl("https://example.com/")
            .setCode(0, 1)
            .build()
    }

    @Test
    fun `load returns null when service reports a business error`() = runBlocking {
        val repository = NewsDetailRepository(CodeErrorRollApi())

        val detail = repository.load("100")

        assertNull(detail)
    }

    private class CodeErrorRollApi : RollApi {
        override suspend fun newsTypeList(appId: String, appSecret: String): RollResponse<NewsTypeListBean> =
            error("not used")

        override suspend fun newsList(
            appId: String,
            appSecret: String,
            type: String,
            page: String,
        ): RollResponse<NewsListBean> = error("not used")

        override suspend fun newsDetail(
            appId: String,
            appSecret: String,
            newsId: String,
        ): RollResponse<NewsDetailBean> = RollResponse(null, 500, "服务繁忙")
    }
}
