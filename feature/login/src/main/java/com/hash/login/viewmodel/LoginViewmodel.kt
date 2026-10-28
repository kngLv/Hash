package com.hash.login.viewmodel

import androidx.lifecycle.viewModelScope
import com.hash.common.base.viewmodel.BaseViewModel
import com.hash.common.shared.session.LoginSession
import com.hash.login.data.repository.LoginRepository

class LoginViewmodel(val repository: LoginRepository = LoginRepository.instance) : BaseViewModel() {

    val loginState = LoginSession.loginStateFlow


    fun login(userName: String, password: String) {
        repository.login(viewModelScope, userName, password)
    }

}
