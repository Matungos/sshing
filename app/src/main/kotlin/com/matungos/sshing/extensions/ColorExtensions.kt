package com.matungos.sshing.extensions

import android.graphics.Color

fun Int.getContrastColor(): Int {
    // Counting the perceptive luminance - human eye favors green color...
    val a = 1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
    return if (a < 0.5) Color.BLACK else Color.WHITE
}
