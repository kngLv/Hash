package com.hash.repository

import com.hash.net.net.LvHttp

/**
 * Created by KngLv
 * @time 2025/12/29 09:49
 * @description
 */

val wanCommonApi by lazy { LvHttp.createApi(WanCommonServiceApi::class.java) }
