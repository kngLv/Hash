package com.hash.router

/**
 * @name RouterActivityPath
 * @package com.hash.router
 * @author 345 QQ:1831712732
 * @time 2024/12/15 20:04
 * @description
 */
class RouterActivityPath {
    object Main {
        private const val ROOT = "/main"

        /** 主页 */
        const val MAIN = "${ROOT}/main"

    }


    object Release {
        private const val ROOT = "/release"
        /** 发布 */
        const val RELEASE = "${ROOT}/release"
    }

    object Login {
        private const val ROOT = "/login"
        /** 登录 */
        const val LOGIN = "${ROOT}/account_login"
    }

    object Floorplan {
        private const val ROOT = "/floorplan"

        /** 户型编辑器 */
        const val EDITOR = "${ROOT}/editor"
    }
}
