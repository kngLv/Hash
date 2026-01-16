package com.hash.repository.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hash.bean.mine.UserInfoBean
import com.hash.common.manager.BuglyCrashManager
import com.hash.common.storage.userInfo.UserInfoStore
import com.hash.net.net.launch.request
import com.hash.net.net.request.onBodyOf
import com.hash.repository.login.api.wanLoginApi
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
            .onBodyOf {
                // 保存用户信息和Cookie
                UserInfoStore.saveUserInfo(UserInfoBean(null, it))
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
        (!UserInfoStore.getCookie()
            .isNullOrEmpty() && UserInfoStore.getUserInfoCached() != null).let {
            return if (it) {
                LoginState.LOGGED
            } else {
                LoginState.UN_LOGIN
            }
        }
    }

}