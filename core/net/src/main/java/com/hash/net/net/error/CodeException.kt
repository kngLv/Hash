package com.hash.net.net.error

import java.lang.Exception

/**
 * @name CodeException
 * @package com.www.net.error
 * @author 345 QQ:1831712732
 * @time 2020/6/27 18:25
 * @description
 */
class CodeException(val code: Int, private val msg: String) :
    Exception("$code $msg") {
    override fun toString(): String {
        return "CodeException(code=$code, msg='$msg')"
    }
}