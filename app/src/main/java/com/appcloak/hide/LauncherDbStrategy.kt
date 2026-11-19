package com.appcloak.hide

import com.appcloak.core.RootManager
import com.appcloak.data.LauncherDbConfig

/**
 * 只改桌面数据库：应用仍可运行，仅从抽屉隐藏。
 *
 * 这是「扩展入口」——每条 SQL 由用户按自己的桌面填写，本类负责安全地拼接命令并执行。
 * 需要设备上存在 sqlite3 可执行文件（可通过 Magisk 模块安装）。
 */
class LauncherDbStrategy(
    private val configProvider: () -> LauncherDbConfig?,
) : HideStrategy {

    override val id = "launcher_db"
    override val label = "桌面数据库"

    private fun currentConfig(): LauncherDbConfig =
        configProvider() ?: throw IllegalStateException("尚未选择桌面数据库配置")

    private fun dbPath(config: LauncherDbConfig): String {
        val safePackage = RootManager.validatePackage(config.launcherPackage)
            ?: throw IllegalArgumentException("桌面包名不合法")
        return "/data/data/$safePackage/${config.dbPath.trimStart('/')}"
    }

    private fun sqliteCommand(config: LauncherDbConfig, sql: String): String =
        "sqlite3 ${RootManager.shellQuote(dbPath(config))} ${RootManager.shellQuote(sql)}"

    private fun fill(sql: String, packageName: String): String {
        val safe = RootManager.validatePackage(packageName)
            ?: throw IllegalArgumentException("非法包名")
        return sql.replace("{pkg}", safe)
    }

    override suspend fun listHidden(): Set<String> {
        val config = configProvider() ?: return emptySet()
        val result = RootManager.execute(sqliteCommand(config, config.querySql))
        if (!result.success) throw RuntimeException(result.message.ifBlank { "sqlite3 查询失败" })
        return result.output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { RootManager.validatePackage(it) }
            .toSet()
    }

    override suspend fun hide(packageName: String): Result<Unit> = runCatching {
        val config = currentConfig()
        val result = RootManager.execute(sqliteCommand(config, fill(config.hideSql, packageName)))
        if (!result.success) throw RuntimeException(result.message.ifBlank { "隐藏 SQL 执行失败" })
        // 部分桌面需要重启桌面进程才刷新
        RootManager.execute("am force-stop ${config.launcherPackage}")
        Unit
    }

    override suspend fun unhide(packageName: String): Result<Unit> = runCatching {
        val config = currentConfig()
        val result = RootManager.execute(sqliteCommand(config, fill(config.unhideSql, packageName)))
        if (!result.success) throw RuntimeException(result.message.ifBlank { "恢复 SQL 执行失败" })
        RootManager.execute("am force-stop ${config.launcherPackage}")
        Unit
    }

    /** 测试某份配置能否正常查询，返回查到的行数。 */
    suspend fun test(config: LauncherDbConfig): Result<Int> = runCatching {
        val result = RootManager.execute(sqliteCommand(config, config.querySql))
        if (!result.success) throw RuntimeException(result.message.ifBlank { "sqlite3 查询失败" })
        result.output.lineSequence().count { it.isNotBlank() }
    }

    suspend fun isSqlite3Available(): Boolean =
        RootManager.execute("command -v sqlite3").success
}
