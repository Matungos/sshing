package com.matungos.sshing.extensions

/**
 * Created by Gabriel on 29/04/2021.
 */

val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        return if (tag.length <= 23) tag else tag.substring(0, 23)
    }
