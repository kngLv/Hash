package com.hash.login.data.repository

import com.hash.common.manager.BuglyCrashManager
import com.hash.net.launch.request
import com.hash.common.shared.session.LoginSession
import com.hash.common.shared.model.UserInfoBean
import com.hash.common.shared.storage.UserSessionStore
import com.hash.login.data.api.loginApi
import kotlinx.coroutines.CoroutineScope

class LoginRepository {
    companion object {
        val instance by lazy { LoginRepository() }
    }


    fun login(coroutineScope: CoroutineScope, userName: String, password: String) {
        LoginSession.markLogging()
        request { loginApi.login(userName, password) }
            .onBody {
                // 保存用户信息和Cookie
                UserSessionStore.saveUserInfo(UserInfoBean(null, it))
                BuglyCrashManager.setUserId("${it.id}")
            }.onEnd {
                // 刷新登录状态
                LoginSession.refreshLoginState()
            }
            .enqueue(coroutineScope)
    }

}
