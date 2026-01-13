package com.hash.repository

import com.hash.net.net.LvHttp

/**
 * Created by KngLv
 * @time 2026/1/5 09:14
 * @description
 */

val rollApi by lazy { LvHttp.createApi(RollServiceApi::class.java) }
