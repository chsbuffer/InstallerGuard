package io.github.chsbuffer.installer

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam

class HookInit : XposedModule() {
    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        val prefs = getRemotePreferences(RemotePrefs.GROUP)
        InstallIntent(this, param.classLoader, prefs)
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean = true

    override fun onHotReloaded(param: HotReloadedParam) {
        val prefs = getRemotePreferences(RemotePrefs.GROUP)
        InstallIntent.updateState(prefs)
        for (old in param.oldHookHandles) {
            if (old.id == "install_intent") {
                val classLoader = old.executable.declaringClass.classLoader
                    ?: error("Boot classloader — cannot resolve fields")
                old.replaceHook(InstallIntent.createHooker(classLoader))
            } else {
                old.unhook()
            }
        }
    }
}
