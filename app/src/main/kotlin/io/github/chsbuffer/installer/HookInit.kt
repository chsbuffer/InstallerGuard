package io.github.chsbuffer.installer

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class HookInit : XposedModule() {

    lateinit var systemServerCL: ClassLoader

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log("onSystemServerStarting: cl=${param.classLoader}")
        systemServerCL = param.classLoader
        val prefs = getRemotePreferences(RemotePrefs.GROUP)
        InstallIntent(this, param.classLoader, prefs)
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        log("onHotReloading — old code")
        param.setSavedInstanceState(systemServerCL)
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        log("onHotReloaded: isSystemServer=${param.isSystemServer}, processName=${param.processName}")
        log("oldHookHandles count ${param.oldHookHandles.count()}")

        param.oldHookHandles.forEach { it.unhook() }

        systemServerCL = param.savedInstanceState as ClassLoader
        val prefs = getRemotePreferences(RemotePrefs.GROUP)
        InstallIntent(this, systemServerCL, prefs)
    }
}