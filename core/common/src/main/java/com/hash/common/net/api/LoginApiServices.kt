package com.hash.common.net.api

import com.hash.net.LvHttp

val wanLoginApi by lazy { LvHttp.createApi(WanLoginServiceApi::class.java) }
