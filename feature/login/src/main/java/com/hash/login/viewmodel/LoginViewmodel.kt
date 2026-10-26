package com.hash.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.common.shared.session.LoginSession
import com.hash.login.data.repository.LoginRepository

class LoginViewmodel(val repository: LoginRepository = LoginRepository.instance) : ViewModel() {

    val loginState = LoginSession.loginStateFlow


    fun login(userName: String, password: String) {
        repository.login(viewModelScope, userName, password)
    }

}
