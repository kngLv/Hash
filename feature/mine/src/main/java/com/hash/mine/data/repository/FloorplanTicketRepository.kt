package com.hash.mine.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.hash.common.shared.storage.UserSessionStore
import com.hash.router.FloorplanIdentityProvider
import com.hash.router.FloorplanTicketResult
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object FloorplanCloudConfig {
    const val ticketEndpoint =
        "https://hash-kng-d1geb954se5651c73-1258197985.ap-shanghai.app.tcloudbase.com/floorplan-ticket"
}

internal class CloudBaseTicketRequester(
    private val postJson: suspend (endpoint: String, body: String) -> String = ::postJson,
) {

    suspend fun request(cookie: String): String {
        val response = postJson(
            FloorplanCloudConfig.ticketEndpoint,
            Gson().toJson(mapOf("cookie" to cookie)),
        )
        return JsonParser.parseString(response)
            .asJsonObject
            .get("ticket")
            ?.asString
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("CloudBase 未返回 Ticket")
    }

}

private val cloudBaseHttpClient = OkHttpClient()

private suspend fun postJson(endpoint: String, body: String): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(endpoint)
        .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    cloudBaseHttpClient.newCall(request).execute().use { response ->
        val responseBody = response.body?.string()
        if (responseBody ==null ||!response.isSuccessful) {
            throw IOException("CloudBase Ticket 请求失败：${response.code}")
        }
        responseBody
    }
}

/**
 * 将 Hash 已保存的 WanAndroid 登录态交给受信任的云函数，并只向调用者返回 CloudBase Ticket。
 */
class FloorplanTicketRepository(
    private val cookieProvider: () -> String? = UserSessionStore::getCookie,
    private val ticketRequester: suspend (cookie: String) -> String = CloudBaseTicketRequester()::request,
) : FloorplanIdentityProvider {

    override suspend fun issueCloudBaseTicket(): FloorplanTicketResult {
        val cookie = cookieProvider()?.takeIf { it.isNotBlank() }
            ?: return FloorplanTicketResult.NotLoggedIn

        return try {
            val ticket = ticketRequester(cookie)
            if (ticket.isBlank()) {
                FloorplanTicketResult.Failed("未获取到户型同步凭据")
            } else {
                FloorplanTicketResult.Success(ticket)
            }
        } catch (_: Throwable) {
            FloorplanTicketResult.Failed("户型同步登录失败，请重新登录后再试")
        }
    }
}
