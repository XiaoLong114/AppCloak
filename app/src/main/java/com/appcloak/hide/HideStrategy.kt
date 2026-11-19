package com.appcloak.hide

/**
 * 一种隐藏策略。两种不同的隐藏方式实现同一接口，UI 层无需关心细节。
 */
interface HideStrategy {
    val id: String
    val label: String

    /** 返回当前处于隐藏状态的包名集合。 */
    suspend fun listHidden(): Set<String>

    suspend fun hide(packageName: String): Result<Unit>

    suspend fun unhide(packageName: String): Result<Unit>
}
