package com.hash.repository

import com.hash.net.LvHttp

val rollApi by lazy { LvHttp.createApi(RollServiceApi::class.java) }
val wanCommonApi by lazy { LvHttp.createApi(WanCommonServiceApi::class.java) }
