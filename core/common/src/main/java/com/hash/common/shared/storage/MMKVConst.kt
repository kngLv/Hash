package com.hash.common.shared.storage

/**
 * 跨模块复用的 MMKV 持久化 Key。
 *
 * 新增 Key 时按所属业务分组，避免业务模块之间复用同一个字符串常量。
 */
object MMKVConst {

    /** 用户 id  */
    const val KEY_BASE_USER_ID = "key_base_user_id"

    /** cookie  */
    const val KEY_BASE_COOKIE = "key_base_cookie"

    /** 置顶新闻 type 列表  */
    const val KEY_FEATURED_NEWS_TYPE_LIST = "key_featured_news_type_list"


    /** 隐私政策同意结果  */
    const val KEY_AGREE_PRIVACY_RESULT = "key_agree_privacy_result"
}
