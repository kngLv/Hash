package com.hash.router

/**
 * 向户型模块提供已登录 Hash 用户对应的 CloudBase 自定义登录票据。
 *
 * 票据只能由服务端签发；实现方不得向调用方暴露 WanAndroid Cookie 或 CloudBase 私钥。
 */
interface FloorplanIdentityProvider {
    suspend fun issueCloudBaseTicket(): FloorplanTicketResult
}

sealed interface FloorplanTicketResult {
    data class Success(val ticket: String) : FloorplanTicketResult

    data object NotLoggedIn : FloorplanTicketResult

    data class Failed(val message: String) : FloorplanTicketResult
}
