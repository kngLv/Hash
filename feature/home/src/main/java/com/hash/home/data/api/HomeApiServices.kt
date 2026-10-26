package com.hash.home.data.api

import com.hash.net.LvHttp

val rollApi by lazy { LvHttp.createApi(RollApi::class.java) }
val homeApi by lazy { LvHttp.createApi(HomeApi::class.java) }
