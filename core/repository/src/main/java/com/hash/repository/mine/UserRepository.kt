package com.hash.repository.mine

import com.hash.bean.mine.UserInfoBean
import com.hash.common.storage.userInfo.UserInfoStore
import com.hash.net.net.launch.request
import com.hash.net.net.request.RequestActionImpl
import com.hash.net.net.request.ResultState
import com.hash.net.net.request.onBodyOf
import com.hash.net.response.WanResponse
import com.hash.repository.mine.api.wanMineApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * UserInfoRepository ：使用 `wanMineApi` 和 `UserInfoStore`。
 * */
class UserInfoRepository {

    companion object {
        val instance by lazy { UserInfoRepository() }
    }
    private val _userInfo = MutableStateFlow(UserInfoStore.getUserInfoCached())
    val userInfoFlow: StateFlow<UserInfoBean?> = _userInfo

    /**
     * 刷新UserInfo
     */
    fun refreshUserInfo(coroutineScope: CoroutineScope) {
        request { wanMineApi.userInfo() }
            .onBodyOf {
                println("=======================> UserInfoRepository refreshUserInfo onBodyOf")
                _userInfo.value = it
                UserInfoStore.saveUserInfo(it)
            }
            .enqueue(coroutineScope)
    }

    fun clear() {
        UserInfoStore.clearUserInfo()
        _userInfo.value = null
    }
}