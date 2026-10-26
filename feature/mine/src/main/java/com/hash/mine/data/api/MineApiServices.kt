package com.hash.mine.data.api

import com.hash.net.LvHttp

val mineApi by lazy { LvHttp.createApi(MineApi::class.java) }
