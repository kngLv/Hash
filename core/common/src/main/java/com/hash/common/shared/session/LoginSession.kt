package com.hash.common.shared.session

import com.hash.common.shared.storage.UserSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LoginState {
    /** 未登录 */
    UN_LOGIN,

    /** 登录中 */
    LOGGING,

    /** 已登录 */
    LOGGED,
}

/** 跨业务共享的登录状态，不包含任何业务接口调用。 */
object LoginSession {
    private val loginState = MutableStateFlow(currentLoginState())

    val loginStateFlow: StateFlow<LoginState> = loginState.asStateFlow()

    fun markLogging() {
        loginState.value = LoginState.LOGGING
    }

    fun refreshLoginState() {
        loginState.value = currentLoginState()
    }

    private fun currentLoginState(): LoginState =
        if (!UserSessionStore.getCookie().isNullOrEmpty() &&
            UserSessionStore.getUserInfoCached() != null
        ) {
            LoginState.LOGGED
        } else {
            LoginState.UN_LOGIN
        }
}
