package com.appcloak.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 本地配置与隐藏记录的持久化。使用系统 SharedPreferences + JSON，避免额外依赖。 */
class PreferencesStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: HideMode
        get() = HideMode.from(prefs.getString(KEY_MODE, null))
        set(value) = prefs.edit().putString(KEY_MODE, value.value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var activeConfigId: String?
        get() = prefs.getString(KEY_ACTIVE_CONFIG, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_CONFIG, value).apply()

    fun hiddenRecords(): List<HiddenRecord> = try {
        val arr = JSONArray(prefs.getString(KEY_HIDDEN, "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            HiddenRecord(
                packageName = obj.getString("pkg"),
                label = obj.optString("label", obj.getString("pkg")),
                mode = HideMode.from(obj.optString("mode", null)),
                hiddenAt = obj.optLong("at", System.currentTimeMillis()),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveHiddenRecords(records: List<HiddenRecord>) {
        val arr = JSONArray()
        records.forEach { record ->
            arr.put(
                JSONObject()
                    .put("pkg", record.packageName)
                    .put("label", record.label)
                    .put("mode", record.mode.value)
                    .put("at", record.hiddenAt),
            )
        }
        prefs.edit().putString(KEY_HIDDEN, arr.toString()).apply()
    }

    fun configs(): List<LauncherDbConfig> = try {
        val arr = JSONArray(prefs.getString(KEY_CONFIGS, "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            LauncherDbConfig(
                id = obj.getString("id"),
                name = obj.getString("name"),
                launcherPackage = obj.getString("launcherPackage"),
                dbPath = obj.getString("dbPath"),
                querySql = obj.getString("querySql"),
                hideSql = obj.getString("hideSql"),
                unhideSql = obj.getString("unhideSql"),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveConfigs(configs: List<LauncherDbConfig>) {
        val arr = JSONArray()
        configs.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("launcherPackage", c.launcherPackage)
                    .put("dbPath", c.dbPath)
                    .put("querySql", c.querySql)
                    .put("hideSql", c.hideSql)
                    .put("unhideSql", c.unhideSql),
            )
        }
        prefs.edit().putString(KEY_CONFIGS, arr.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "appcloak_prefs"
        const val KEY_MODE = "mode"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_HIDDEN = "hidden_records"
        const val KEY_CONFIGS = "launcher_configs"
        const val KEY_ACTIVE_CONFIG = "active_config_id"
    }
}
