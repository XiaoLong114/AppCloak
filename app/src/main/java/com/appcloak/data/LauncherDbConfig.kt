package com.appcloak.data

/**
 * 桌面数据库隐藏方案的配置。
 *
 * 不同桌面（Launcher3 / MIUI / EMUI / ColorOS ...）的库表和字段差异很大，
 * 这里不预设“万能 SQL”，只提供一个可编辑的模板，由用户按自己设备填写。
 *
 * SQL 中的 {pkg} 会被替换为经过白名单校验的包名。
 */
data class LauncherDbConfig(
    val id: String,
    val name: String,
    val launcherPackage: String,
    /** 相对 /data/data/<launcherPackage>/ 的路径，例如 databases/launcher.db */
    val dbPath: String,
    /** 查询当前已隐藏的应用，输出每行一个包名 */
    val querySql: String,
    val hideSql: String,
    val unhideSql: String,
) {
    companion object {
        fun sample(): LauncherDbConfig = LauncherDbConfig(
            id = "sample",
            name = "示例模板（请按实机修改）",
            launcherPackage = "com.android.launcher3",
            dbPath = "databases/launcher.db",
            querySql = "SELECT package_name FROM hidden_apps;",
            hideSql = "INSERT OR REPLACE INTO hidden_apps(package_name) VALUES('{pkg}');",
            unhideSql = "DELETE FROM hidden_apps WHERE package_name='{pkg}';",
        )

        fun newId(): String = "cfg_" + System.currentTimeMillis().toString(36)
    }
}
