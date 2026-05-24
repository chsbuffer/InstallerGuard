package io.github.chsbuffer.installer

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface

class InstallIntent(xposed: XposedInterface, classLoader: ClassLoader, prefs: SharedPreferences) {
    init {
        log("InstallIntent: initializing hook in system_server")

        updateState(prefs)
        // Listen for preference changes and update cached state
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            log("InstallIntent: preference changed: $key")
            updateState(prefs)
        }

        val cls = classLoader.loadClass("com.android.server.wm.ActivityStarter")
        log("InstallIntent: loaded class $cls")
        val executeMethod = cls.getDeclaredMethod("execute")
        log("InstallIntent: found method $executeMethod")

        xposed.hook(executeMethod)
            .intercept(createHooker(classLoader))

        log("InstallIntent: hook registered successfully")
    }

    companion object {
        @Volatile
        private var enabled = true

        @Volatile
        private lateinit var component: ComponentName

        fun updateState(prefs: SharedPreferences) {
            enabled = RemotePrefs.isEnabled(prefs)
            component = RemotePrefs.getComponent(prefs)
            log("InstallIntent: state updated — enabled=$enabled, component=${component.flattenToShortString()}")
        }

        fun createHooker(classLoader: ClassLoader): XposedInterface.Hooker {
            val cls = classLoader.loadClass("com.android.server.wm.ActivityStarter")
            val requestField = cls.getDeclaredField("mRequest").apply { isAccessible = true }
            val requestClass =
                classLoader.loadClass("com.android.server.wm.ActivityStarter\$Request")
            val intentField = requestClass.getDeclaredField("intent").apply { isAccessible = true }
            log("InstallIntent: createHooker — requestField=$requestField, intentField=$intentField")

            return XposedInterface.Hooker { chain ->
                try {
                    if (!enabled) {
                        return@Hooker chain.proceed()
                    }

                    val requestObj = requestField.get(chain.thisObject)
                    val intent = intentField.get(requestObj) as Intent

                    if (isInstallIntent(intent)) {
                        log("InstallIntent: matched install intent")
                        log("InstallIntent: ${describeIntent(intent)}")
                        if (intent.component == null) {
                            intent.component = component
                            intentField.set(requestObj, intent)
                            log("InstallIntent: set component to ${component.flattenToShortString()}")
                        } else {
                            log("InstallIntent: component already set: ${intent.component?.flattenToShortString()}, skipping")
                        }
                    }
                } catch (t: Throwable) {
                    log("InstallIntent: hook error — ${t.message}")
                    log(t)
                }
                chain.proceed()
            }
        }

        private fun describeIntent(intent: Intent): String = buildString {
            append("action=${intent.action}")
            append(", type=${intent.type}")
            append(", scheme=${intent.scheme}")
            append(", data=${intent.dataString}")
            append(", component=${intent.component?.flattenToShortString()}")
            append(", flags=0x${intent.flags.toString(16)}")
        }

        private fun isInstallIntent(intent: Intent): Boolean =
            intent.action == "android.intent.action.INSTALL_PACKAGE" ||
                    (intent.type == "application/vnd.android.package-archive" &&
                            intent.action != "android.intent.action.CREATE_DOCUMENT" &&
                            intent.action != "android.intent.action.OPEN_DOCUMENT" &&
                            intent.action != "android.intent.action.GET_CONTENT")
    }
}
