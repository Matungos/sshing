package com.matungos.sshing.bindingadapters

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import androidx.databinding.BindingConversion
import androidx.databinding.InverseMethod


/**
 * Created by Gabriel on 15/04/2021.
 */
object Converters {

    @BindingConversion
    @JvmStatic
    fun convertColorToDrawable(color: Int): ColorDrawable {
        return ColorDrawable(color)
    }

    @BindingConversion
    @JvmStatic
    fun convertColorToColorStateList(color: Int): ColorStateList {
        return ColorStateList.valueOf(color)
    }


    @JvmStatic
    fun convertStringToInt(text: String): Int {
        var result = 0
        try {
            result = Integer.parseInt(text)
        } catch (e: NumberFormatException) {
        }
        return result
    }

    @InverseMethod(value = "convertStringToInt")
    @JvmStatic
    fun convertIntToString(value: Int): String {
        return value.toString()
    }

}