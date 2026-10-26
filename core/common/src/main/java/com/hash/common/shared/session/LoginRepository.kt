package com.hash.common.shared.session

import com.hash.common.manager.BuglyCrashManager
import com.hash.net.launch.request
import com.hash.common.net.api.wanLoginApi
import com.hash.common.shared.model.UserInfoBean
import com.hash.common.shared.storage.UserSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class LoginState {
    /** 未登录 */
    UN_LOGIN,

    /** 登录中 */
    LOGGING,

    /** 已登录 */
    LOGGED
}

class LoginRepository {
    private val loginState = MutableStateFlow(getLoginState())

    val loginStateFlow: StateFlow<LoginState> = loginState

    companion object {
        val instance by lazy { LoginRepository() }
    }


    fun login(coroutineScope: CoroutineScope, userName: String, password: String) {
        loginState.value = LoginState.LOGGING
        request { wanLoginApi.login(userName, password) }
            .onBody {
                // 保存用户信息和Cookie
                UserSessionStore.saveUserInfo(UserInfoBean(null, it))
                BuglyCrashManager.setUserId("${it.id}")
            }.onEnd {
                // 刷新登录状态
                refreshLoginState()
            }
            .enqueue(coroutineScope)
    }

    fun refreshLoginState() {
        loginState.value = getLoginState()
    }

    fun getLoginState(): LoginState {
        (!UserSessionStore.getCookie()
            .isNullOrEmpty() && UserSessionStore.getUserInfoCached() != null).let {
            return if (it) {
                LoginState.LOGGED
            } else {
                LoginState.UN_LOGIN
            }
        }
    }

}
