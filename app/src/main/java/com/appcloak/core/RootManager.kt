package com.appcloak.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过 su 执行 root 命令。
 *
 * 安全约定：所有由用户数据拼接进命令的位置，都必须先经过校验/转义。
 * 包名走 [validatePackage] 白名单；SQL 走 [shellQuote]。
 */
object RootManager {

    private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_.]*$")

    @Volatile
    private var checked = false

    @Volatile
    private var available = false

    /** 检测 su 是否可用且已授权。结果会被缓存，需要重新检测请调用 [resetCheck]。 */
    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        val result = executeInternal("id")
        available = result.success && result.output.contains("uid=0")
        checked = true
        available
    }

    fun isRootReady(): Boolean = checked && available

    fun resetCheck() {
        checked = false
        available = false
    }

    /** 执行一条 root 命令，不会抛异常，失败信息在 [ShellResult.message]。 */
    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        executeInternal(command)
    }

    private fun executeInternal(command: String): ShellResult = try {
        val process = ProcessBuilder("su", "-c", command)
            // 合并输出流，避免 stderr 缓冲区写满导致进程死锁
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val exitCode = process.waitFor()
        ShellResult(exitCode == 0, output, exitCode)
    } catch (t: Throwable) {
        ShellResult(false, t.message ?: t.toString(), -1)
    }

    /** 包名白名单校验：只允许字母开头 + 字母数字点下划线，阻断 shell 注入。 */
    fun validatePackage(packageName: String): String? =
        packageName.takeIf { it.length in 1..200 && PACKAGE_REGEX.matches(it) }

    /** POSIX shell 单引号转义，用于把任意字符串安全地嵌入命令。 */
    fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
