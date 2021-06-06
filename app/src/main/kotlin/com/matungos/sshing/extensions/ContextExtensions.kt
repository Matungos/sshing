package com.matungos.sshing.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.res.use

/**
 * Created by Gabriel on 25/04/2021.
 */

fun Context.shortToast(@StringRes text: Int) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
fun Context.shortToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
fun Context.longToast(@StringRes text: Int) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
fun Context.longToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

/**
 * Retrieve a color from the current [android.content.res.Resources.Theme].
 */
@ColorInt
@SuppressLint("Recycle")
fun Context.themeColor(
    @AttrRes themeAttrId: Int
): Int {
    return obtainStyledAttributes(
        intArrayOf(themeAttrId)
    ).use {
        it.getColor(0, Color.MAGENTA)
    }
}
