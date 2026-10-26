package com.hash.mine.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.common.shared.model.UserInfoBean
import com.hash.net.launch.requestRaw
import com.hash.common.net.api.wanMineApi
import com.hash.common.shared.session.LoginRepository
import com.hash.common.shared.session.UserInfoRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MineViewModel(
    private val userInfoRepository: UserInfoRepository = UserInfoRepository.instance
) : ViewModel() {

    val tabList = arrayListOf("笔记", "评论", "收藏", "赞过")

    val userInfoFlow: StateFlow<UserInfoBean?> = userInfoRepository.userInfoFlow

    val loginState = LoginRepository.instance.loginStateFlow


    suspend fun refreshUserInfo() {
        requestRaw { wanMineApi.test() }
            .onBody {
                println("test result: $it")
            }
            .enqueue(viewModelScope)
        userInfoRepository.refreshUserInfo(viewModelScope)
    }
}
