package com.hash.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.repository.login.LoginRepository

class LoginViewmodel(val repository: LoginRepository = LoginRepository.instance) : ViewModel() {

    val loginState = repository.loginStateFlow


    fun login(userName: String, password: String) {
        repository.login(viewModelScope, userName, password)
    }

}