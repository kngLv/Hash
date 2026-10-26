package com.hash.mine.data.repository

import com.hash.router.FloorplanTicketResult
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorplanTicketRepositoryTest {

    @Test
    fun `未登录时不请求云函数并返回未登录结果`() = runBlocking {
        var requested = false
        val repository = FloorplanTicketRepository(
            cookieProvider = { null },
            ticketRequester = {
                requested = true
                error("不应请求")
            },
        )

        assertEquals(FloorplanTicketResult.NotLoggedIn, repository.issueCloudBaseTicket())
        assertFalse(requested)
    }

    @Test
    fun `有效登录态只将 Ticket 返回给调用方`() = runBlocking {
        val repository = FloorplanTicketRepository(
            cookieProvider = { "loginUserName=test; token_pass=test" },
            ticketRequester = { cookie ->
                assertEquals("loginUserName=test; token_pass=test", cookie)
                "signed-ticket"
            },
        )

        assertEquals(
            FloorplanTicketResult.Success("signed-ticket"),
            repository.issueCloudBaseTicket(),
        )
    }

    @Test
    fun `请求器将 Cookie 发往 CloudBase 网关并解析 Ticket`() = runBlocking {
        var requestedEndpoint = ""
        var requestBody = ""
        val requester = CloudBaseTicketRequester { endpoint, body ->
            requestedEndpoint = endpoint
            requestBody = body
            "{\"ticket\":\"signed-ticket\"}"
        }

        assertEquals("signed-ticket", requester.request("token_pass=abc"))
        assertEquals(FloorplanCloudConfig.ticketEndpoint, requestedEndpoint)
        assertEquals(
            "token_pass=abc",
            JsonParser.parseString(requestBody).asJsonObject["cookie"].asString,
        )
        assertTrue(requestBody.contains("cookie"))
    }
}
