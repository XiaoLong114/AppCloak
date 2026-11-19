package com.appcloak.core

/**
 * 一次 shell 命令的执行结果。stdout 与 stderr 已合并，便于展示。
 */
data class ShellResult(
    val success: Boolean,
    val output: String,
    val exitCode: Int,
) {
    val message: String get() = output.trim()
}
