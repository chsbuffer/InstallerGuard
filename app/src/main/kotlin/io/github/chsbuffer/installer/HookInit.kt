package io.github.chsbuffer.installer

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class HookInit : XposedModule() {
    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        val prefs = getRemotePreferences(RemotePrefs.GROUP)
        InstallIntent(this, param.classLoader, prefs)
    }
}
