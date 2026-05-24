package io.github.chsbuffer.installer

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.isFirstApplication || lpparam.classLoader == null) return
        InstallIntent(lpparam).start()
    }
}
