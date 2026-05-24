package io.github.chsbuffer.installer

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class Hook(val lpparam: XC_LoadPackage.LoadPackageParam) {
    companion object {
        fun getClass(cls: Class<*>, classLoader: ClassLoader): Class<*> =
            XposedHelpers.findClassIfExists(cls.name, classLoader)

        fun getClass(className: String, classLoader: ClassLoader): Class<*> =
            XposedHelpers.findClassIfExists(className, classLoader)

        fun isPackageName(lpparam: XC_LoadPackage.LoadPackageParam, packageName: String): Boolean =
            lpparam.packageName == packageName

        fun isProcessName(lpparam: XC_LoadPackage.LoadPackageParam, processName: String): Boolean =
            lpparam.processName == processName
    }

    open fun beforeHook(): Boolean = true
    open fun afterHook() {}
    abstract fun hooking()

    fun getClass(cls: Class<*>): Class<*> = Companion.getClass(cls, lpparam.classLoader!!)
    fun getClass(className: String): Class<*> = Companion.getClass(className, lpparam.classLoader!!)

    fun isPackageName(packageName: String): Boolean = Companion.isPackageName(lpparam, packageName)
    fun isProcessName(processName: String): Boolean = Companion.isProcessName(lpparam, processName)

    fun start() {
        if (beforeHook()) {
            hooking()
            afterHook()
        }
    }
}
