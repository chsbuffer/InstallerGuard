package io.github.chsbuffer.installer

import android.content.ComponentName
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class InstallIntent(lpparam: XC_LoadPackage.LoadPackageParam) : Hook(lpparam) {

    override fun beforeHook(): Boolean = isPackageName("android")

    override fun hooking() {
        val cls = getClass("com.android.server.wm.ActivityStarter")!!
        val requestField = cls.getDeclaredField("mRequest").apply { isAccessible = true }
        val requestClass = getClass("com.android.server.wm.ActivityStarter\$Request")!!
        val intentField = requestClass.getDeclaredField("intent").apply { isAccessible = true }

        XposedHelpers.findAndHookMethod(cls, "execute", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val requestObj = requestField.get(param.thisObject)
                val intent = intentField.get(requestObj) as Intent
                if (isInstallIntent(intent)) {
                    intent.setInstallerComponent()
                    intentField.set(requestObj, intent)
                    log(intent)
                }
                super.beforeHookedMethod(param)
            }
        })
    }

    companion object {
        private fun isInstallIntent(intent: Intent): Boolean =
            intent.action == "android.intent.action.INSTALL_PACKAGE" ||
            intent.type == "application/vnd.android.package-archive"

        private fun Intent.setInstallerComponent() {
            if (component != null) return
            component = ComponentName(
                "io.github.vvb2060.packageinstaller",
                "io.github.vvb2060.packageinstaller.ui.InstallLaunch"
            )
        }
    }
}
