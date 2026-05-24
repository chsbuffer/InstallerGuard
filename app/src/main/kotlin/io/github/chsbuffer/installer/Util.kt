package io.github.chsbuffer.installer

import android.util.Log

fun log(message: Any) {
    val tag = "InstallerGuard"
    when (message) {
        is Throwable -> Log.e(tag, "", message)
        else -> Log.i(tag, message.toString())
    }
}
