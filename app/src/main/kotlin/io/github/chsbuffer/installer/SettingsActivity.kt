@file:Suppress("DEPRECATION")

package io.github.chsbuffer.installer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.HookedProcess
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class SettingsActivity : Activity(), XposedServiceHelper.OnServiceListener {

    private var service: XposedService? = null
    private var adapter: AppAdapter? = null
    private var selected: ComponentName = RemotePrefs.defaultComponent
    private var items: List<ResolveInfo> = emptyList()
    private var frameworkText: TextView? = null
    private var enableCheck: CheckBox? = null
    private var reloadButton: TextView? = null
    private var reloadStatus: TextView? = null

    @SuppressLint("RequestInstallPackagesPolicy")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log("onCreate: querying install intent activities...")
        val seen = mutableSetOf<ComponentName>()
        items = buildList {
            fun addUnique(list: List<ResolveInfo>) {
                for (ri in list) {
                    val cn = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                    if (seen.add(cn)) add(ri)
                }
            }
            // ACTION_INSTALL_PACKAGE + content scheme (system package installer)
            addUnique(
                packageManager.queryIntentActivities(
                    Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(
                        android.net.Uri.parse("content://dummy"),
                        "application/vnd.android.package-archive"
                    ),
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            )
            // ACTION_INSTALL_PACKAGE + file scheme (file managers etc.)
            addUnique(
                packageManager.queryIntentActivities(
                    Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(
                        android.net.Uri.parse("file://dummy"),
                        "application/vnd.android.package-archive"
                    ),
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            )
        }
        for (item in items) {
            val cn = ComponentName(item.activityInfo.packageName, item.activityInfo.name)
            log("found: ${item.loadLabel(packageManager)} ($cn)")
        }
        log("onCreate: total ${items.size} items found")

        adapter = AppAdapter(packageManager)
        val header = buildHeader()
        val listView = ListView(this).apply {
            addHeaderView(header, null, false)
            adapter = this@SettingsActivity.adapter
            setOnItemClickListener { parent, _, position, _ ->
                val info = items[position - (parent as ListView).headerViewsCount]
                selected = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                this@SettingsActivity.adapter?.notifyDataSetChanged()
                saveSelection(selected)
            }
        }

        setContentView(listView)
        // Consume system bar insets so content avoids status/nav bar overlap (edge-to-edge on 15+)
        listView.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            val top = insets.systemWindowInsetTop

            @Suppress("DEPRECATION")
            val bottom = insets.systemWindowInsetBottom
            v.setPadding(v.paddingLeft, top, v.paddingRight, bottom)
            insets
        }
        XposedServiceHelper.registerListener(this)
    }

    context(g: ViewGroup)
    inline fun <T : View> View(factory: (android.content.Context) -> T, block: T.() -> Unit): T {
        return factory(g.context).apply(block).also(g::addView)
    }

    context(g: ViewGroup)
    fun TextView(block: TextView.() -> Unit) = View(::TextView, block)

    context(g: ViewGroup)
    fun CheckBox(block: CheckBox.() -> Unit) = View(::CheckBox, block)

    private fun ViewGroup.separator() {
        val v = android.view.View(context).apply {
            background = resources.getDrawable(android.R.drawable.divider_horizontal_bright, theme)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).apply { setMargins(0, dp(8), 0, dp(8)) }
        }
        addView(v)
    }

    private fun buildHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))

            frameworkText = TextView {
                setText(R.string.framework_waiting)
                textSize = 14f
            }

            TextView {
                text = ""
                setPadding(0, dp(8), 0, dp(8))
            }

            enableCheck = CheckBox {
                setText(R.string.enable_module)
                textSize = 16f
                isEnabled = false
                setOnCheckedChangeListener { _, isChecked ->
                    service?.let {
                        RemotePrefs.setEnabled(
                            it.getRemotePreferences(RemotePrefs.GROUP),
                            isChecked
                        )
                    }
                }
            }

            separator()

            reloadButton = TextView {
                setText(R.string.hot_reload)
                textSize = 16f
                gravity = android.view.Gravity.START
                setPadding(0, dp(8), 0, dp(4))
                setOnClickListener { hotReload() }
            }

            reloadStatus = TextView {
                setText(R.string.hot_reload_hint)
                textSize = 13f
                setTextColor(0xff888888.toInt())
            }

            TextView {
                setText(R.string.select_installer)
                textSize = 16f
                setPadding(0, dp(16), 0, dp(4))
            }
        }
    }

    override fun onServiceBind(svc: XposedService) {
        runOnUiThread {
            service = svc
            val prefs = svc.getRemotePreferences(RemotePrefs.GROUP)
            selected = RemotePrefs.getComponent(prefs)
            enableCheck?.apply {
                isChecked = RemotePrefs.isEnabled(prefs)
                isEnabled = true
            }
            frameworkText?.text = buildString {
                append(svc.frameworkName)
                append(" v")
                append(svc.frameworkVersion)
                append(" (API ")
                append(svc.apiVersion)
                append(")")
            }
            adapter?.notifyDataSetChanged()

            // Enable hot reload button if supported
            val hotReloadEnabled = (svc.frameworkProperties and PROP_RT_HOT_RELOAD) != 0L
            reloadButton?.isEnabled = hotReloadEnabled
            reloadStatus?.setText(
                if (hotReloadEnabled) R.string.hot_reload_hint
                else R.string.hot_reload_unsupported
            )
        }
    }

    override fun onServiceDied(svc: XposedService) {
        runOnUiThread {
            service = null
            enableCheck?.isEnabled = false
            frameworkText?.setText(R.string.framework_disconnected)
            reloadButton?.isEnabled = false
            reloadStatus?.setText(R.string.hot_reload_unsupported)
        }
    }

    private fun saveSelection(component: ComponentName) {
        service?.let {
            RemotePrefs.setComponent(it.getRemotePreferences(RemotePrefs.GROUP), component)
        }
    }

    private fun hotReload() {
        val svc = service ?: return
        if (svc.apiVersion >= 102) {
            if ((svc.frameworkProperties and PROP_RT_HOT_RELOAD) == 0L) {
                Toast.makeText(this, R.string.hot_reload_unsupported, Toast.LENGTH_SHORT).show()
                return
            }

            reloadStatus?.setText(R.string.hot_reload_hint)

            val targets: List<HookedProcess> = try {
                svc.getRunningTargets()
            } catch (e: Exception) {
                log("hotReload: getRunningTargets failed — ${e.message}")
                Toast.makeText(this, "getRunningTargets: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            log("getRunningTargets count ${targets.count()}: ")
            targets.map { target -> "${target.processName} (uid=${target.uid}, pid=${target.pid})" }
                .forEach {
                    log(it)
                }

            if (targets.isEmpty()) {
                Toast.makeText(this, R.string.hot_reload_no_target, Toast.LENGTH_SHORT).show()
                return
            }

            // Prefer the target running as SYSTEM_UID (system_server)
            val target = targets.firstOrNull { it.uid == Process.SYSTEM_UID }
                ?: targets.first()

            log("hotReload: requesting reload of ${target.processName} (uid=${target.uid}, pid=${target.pid})")
            reloadStatus?.text = getString(R.string.framework_waiting)
            reloadButton?.isEnabled = false

            try {
                svc.hotReloadModule(target, null) { _, result ->
                    runOnUiThread {
                        reloadButton?.isEnabled = true
                        when (result.status) {
                            HotReloadResult.Status.SUCCESS -> {
                                reloadStatus?.setText(R.string.hot_reload_success)
                                Toast.makeText(
                                    this@SettingsActivity,
                                    R.string.hot_reload_success, Toast.LENGTH_SHORT
                                ).show()
                            }

                            HotReloadResult.Status.IN_PROGRESS -> {
                                reloadStatus?.setText(R.string.framework_waiting)
                                Toast.makeText(
                                    this@SettingsActivity,
                                    R.string.hot_reload_success, Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                val msg = result.message ?: result.status.name
                                reloadStatus?.text = getString(R.string.hot_reload_failed, msg)
                                Toast.makeText(
                                    this@SettingsActivity,
                                    getString(R.string.hot_reload_failed, msg),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    reloadButton?.isEnabled = true
                    reloadStatus?.setText(R.string.hot_reload_hint)
                    Toast.makeText(this, "hotReloadModule: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PROP_RT_HOT_RELOAD = 8L
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private inner class AppAdapter(private val pm: PackageManager) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = (convertView as? TextView) ?: TextView(this@SettingsActivity).apply {
                setPadding(dp(16), dp(12), dp(16), dp(12))
                textSize = 14f
            }
            val info = items[position]
            val label = info.loadLabel(pm).toString()
            val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            val check = if (component == selected) "✓ " else ""
            view.text = "$check$label\n    ${component.flattenToShortString()}"
            return view
        }
    }
}
