package com.hash.repository.mine.api

import com.hash.net.net.LvHttp

val wanMineApi by lazy { LvHttp.createApi(WanMineServiceApi::class.java) }