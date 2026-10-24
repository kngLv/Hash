package com.hash.repository.login.api

import com.hash.net.LvHttp

val wanLoginApi by lazy { LvHttp.createApi(WanLoginServiceApi::class.java) }
