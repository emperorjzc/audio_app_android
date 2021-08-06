package com.cheng.audio

import android.util.Log

object LogUtil {

    inline fun <reified T> i(message: String) {
        if (!isPrintLog()) return
        Log.i(T::class.java.simpleName, message)
    }

    fun isPrintLog(): Boolean {
        return BuildConfig.DEBUG
    }

}