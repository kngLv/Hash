package com.hash.login.data.api

import com.hash.net.LvHttp

val loginApi by lazy { LvHttp.createApi(LoginApi::class.java) }
