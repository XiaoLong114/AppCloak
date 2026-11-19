package com.appcloak.data

enum class HideMode(val value: String) {
    /** pm disable-user --user 0 <pkg> / pm enable <pkg> */
    PM_DISABLE("pm_disable"),

    /** 修改桌面 App 的 SQLite 数据库 */
    LAUNCHER_DB("launcher_db");

    companion object {
        fun from(value: String?): HideMode = values().find { it.value == value } ?: PM_DISABLE
    }
}
