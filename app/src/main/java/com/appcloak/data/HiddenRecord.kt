package com.appcloak.data

/** 一条隐藏记录。记录当时使用的策略，恢复时按原策略执行。 */
data class HiddenRecord(
    val packageName: String,
    val label: String,
    val mode: HideMode,
    val hiddenAt: Long = System.currentTimeMillis(),
)
