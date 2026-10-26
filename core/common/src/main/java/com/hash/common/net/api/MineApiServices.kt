package com.hash.common.net.api

import com.hash.net.LvHttp

val wanMineApi by lazy { LvHttp.createApi(WanMineServiceApi::class.java) }
