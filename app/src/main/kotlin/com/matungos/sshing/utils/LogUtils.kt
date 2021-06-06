package com.matungos.sshing.utils

import android.util.Log

import com.matungos.sshing.BuildConfig

object LogUtils {

    fun logd(tag: String, message: String) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.LOG_ENABLED || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message)
        }
    }

    fun logd(tag: String, message: String, cause: Throwable) {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (BuildConfig.LOG_ENABLED || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message, cause)
        }
    }

    fun logi(tag: String, message: String) {
        if (BuildConfig.LOG_ENABLED)
            Log.i(tag, message)
    }

    fun logi(tag: String, message: String, cause: Throwable) {
        if (BuildConfig.LOG_ENABLED)
            Log.i(tag, message, cause)
    }

    fun logw(tag: String, message: String) {
        if (BuildConfig.LOG_ENABLED)
            Log.w(tag, message)
    }

    fun logw(tag: String, message: String, cause: Throwable) {
        if (BuildConfig.LOG_ENABLED)
            Log.w(tag, message, cause)
    }

    fun loge(tag: String, message: String) {
        if (BuildConfig.LOG_ENABLED)
            Log.e(tag, message)
    }

    fun loge(tag: String, message: String, cause: Throwable) {
        if (BuildConfig.LOG_ENABLED)
            Log.e(tag, message, cause)
    }
}
