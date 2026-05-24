package io.github.chsbuffer.installer

import android.content.ComponentName
import android.content.SharedPreferences

object RemotePrefs {
    const val GROUP = "settings"
    private const val KEY_COMPONENT = "installer_component"
    private const val KEY_ENABLED = "enabled"

    val defaultComponent = ComponentName(
        "io.github.vvb2060.packageinstaller",
        "io.github.vvb2060.packageinstaller.ui.InstallLaunch"
    )

    fun getComponent(prefs: SharedPreferences): ComponentName {
        val raw = prefs.getString(KEY_COMPONENT, null) ?: return defaultComponent
        return ComponentName.unflattenFromString(raw) ?: defaultComponent
    }

    fun setComponent(prefs: SharedPreferences, component: ComponentName) {
        prefs.edit().putString(KEY_COMPONENT, component.flattenToString()).apply()
    }

    fun isEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
