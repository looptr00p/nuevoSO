package com.nuevoso.launcher.data.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)

class AppRepository(private val pm: PackageManager) {

    fun getAllApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { ri ->
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    icon = ri.loadIcon(pm),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun getLaunchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun findByName(name: String): AppInfo? {
        val query = name.trim().lowercase()
        val apps = getAllApps()
        return apps.firstOrNull { it.label.lowercase() == query }
            ?: apps.firstOrNull { it.label.lowercase().contains(query) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(query) }
    }
}
