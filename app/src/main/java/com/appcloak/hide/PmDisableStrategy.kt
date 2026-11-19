package com.appcloak.hide

import com.appcloak.core.RootManager

/**
 * 通过 pm 禁用/启用应用。root 下最通用可靠的方式。
 *
 * 隐藏：pm disable-user --user 0 <pkg>
 * 恢复：pm enable <pkg>
 * 查询：pm list packages -d --user 0
 */
class PmDisableStrategy : HideStrategy {

    override val id = "pm_disable"
    override val label = "pm disable-user"

    override suspend fun listHidden(): Set<String> {
        val result = RootManager.execute("pm list packages -d --user 0")
        if (!result.success) throw RuntimeException(result.message.ifBlank { "无法读取已禁用应用列表" })
        return result.output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .mapNotNull { RootManager.validatePackage(it) }
            .toSet()
    }

    override suspend fun hide(packageName: String): Result<Unit> {
        val safe = RootManager.validatePackage(packageName)
            ?: return Result.failure(IllegalArgumentException("非法包名"))
        val result = RootManager.execute("pm disable-user --user 0 $safe")
        return if (result.success) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException(result.message.ifBlank { "pm disable-user 执行失败" }))
        }
    }

    override suspend fun unhide(packageName: String): Result<Unit> {
        val safe = RootManager.validatePackage(packageName)
            ?: return Result.failure(IllegalArgumentException("非法包名"))
        val result = RootManager.execute("pm enable $safe")
        return if (result.success) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException(result.message.ifBlank { "pm enable 执行失败" }))
        }
    }
}
