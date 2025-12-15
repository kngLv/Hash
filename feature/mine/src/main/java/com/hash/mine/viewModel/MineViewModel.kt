package com.hash.mine.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.mine.UserInfoBean
import com.hash.repository.login.LoginRepository
import com.hash.repository.mine.UserInfoRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MineViewModel(
    private val userInfoRepository: UserInfoRepository = UserInfoRepository.instance
) : ViewModel() {

    val userInfoFlow: StateFlow<UserInfoBean?> = userInfoRepository.userInfoFlow

    val loginState = LoginRepository.instance.loginStateFlow


    fun refreshUserInfo() {
        viewModelScope.launch {
            userInfoRepository.refreshUserInfo(this)
        }
    }
}