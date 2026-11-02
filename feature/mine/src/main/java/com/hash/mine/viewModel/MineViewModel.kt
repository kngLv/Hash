package com.hash.mine.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.net.launch.requestRaw
import com.hash.common.shared.model.UserInfoBean
import com.hash.common.shared.session.LoginSession
import com.hash.mine.data.api.mineApi
import com.hash.mine.data.repository.UserInfoRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MineViewModel(
    private val userInfoRepository: UserInfoRepository = UserInfoRepository.instance
) : ViewModel() {

    val tabList = arrayListOf("笔记", "评论", "收藏", "赞过")

    val userInfoFlow: StateFlow<UserInfoBean?> = userInfoRepository.userInfoFlow

    val loginState = LoginSession.loginStateFlow


    suspend fun refreshUserInfo() {
        userInfoRepository.refreshUserInfo(viewModelScope)
    }
}
