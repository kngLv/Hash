package com.hash.mine.data.repository

import com.hash.net.launch.request
import com.hash.common.shared.model.UserInfoBean
import com.hash.common.shared.storage.UserSessionStore
import com.hash.mine.data.api.mineApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * UserInfoRepository ：使用 `mineApi` 和 `UserSessionStore`。
 * */
class UserInfoRepository {

    companion object {
        val instance by lazy { UserInfoRepository() }
    }

    private val _userInfo = MutableStateFlow(UserSessionStore.getUserInfoCached())
    val userInfoFlow: StateFlow<UserInfoBean?> = _userInfo

    /**
     * 刷新UserInfo
     */
    suspend fun refreshUserInfo(coroutineScope: CoroutineScope): Boolean =
        suspendCancellableCoroutine { continuation ->
            request { mineApi.userInfo() }
                .onBody {
                    _userInfo.value = it
                    UserSessionStore.saveUserInfo(it)
                }
                .onEnd {
                    continuation.resume(it) { cause, resourceToClose, context ->
                        // No special cleanup required
                    }
                }
                .enqueue(coroutineScope)
        }

    fun clear() {
        UserSessionStore.clearUserInfo()
        _userInfo.value = null
    }
}
