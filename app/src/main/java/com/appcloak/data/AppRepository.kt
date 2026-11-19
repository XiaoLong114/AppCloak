package com.appcloak.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** 读取设备上已安装应用。需要 QUERY_ALL_PACKAGES 权限（Android 11+）。 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val selfPackage: String = context.packageName

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherPackages = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .toHashSet()

        val collator = Collator.getInstance(Locale.getDefault())

        @Suppress("DEPRECATION")
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            // 禁止隐藏本工具自身，否则无法再打开
            .filter { it.packageName != selfPackage }
            .mapNotNull { app: ApplicationInfo ->
                try {
                    AppInfo(
                        packageName = app.packageName,
                        label = app.loadLabel(packageManager).toString().ifBlank { app.packageName },
                        icon = app.loadIcon(packageManager),
                        isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        hasLauncher = launcherPackages.contains(app.packageName),
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedWith(compareBy(collator) { it.label })
            .toList()
    }

    fun labelOf(packageName: String): String = try {
        @Suppress("DEPRECATION")
        val app = packageManager.getApplicationInfo(packageName, 0)
        app.loadLabel(packageManager).toString().ifBlank { packageName }
    } catch (e: Exception) {
        packageName
    }
}
