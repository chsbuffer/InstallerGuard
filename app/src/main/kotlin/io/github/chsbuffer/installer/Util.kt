package io.github.chsbuffer.installer

import de.robv.android.xposed.XposedBridge

fun log(message: Any) {
    when (message) {
        is Throwable -> XposedBridge.log(message)
        else -> XposedBridge.log(message.toString())
    }
}
