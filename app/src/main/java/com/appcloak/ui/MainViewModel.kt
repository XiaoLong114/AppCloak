package com.appcloak.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appcloak.core.RootManager
import com.appcloak.data.AppInfo
import com.appcloak.data.AppRepository
import com.appcloak.data.HideMode
import com.appcloak.data.HiddenRecord
import com.appcloak.data.LauncherDbConfig
import com.appcloak.data.PreferencesStore
import com.appcloak.hide.HideStrategy
import com.appcloak.hide.LauncherDbStrategy
import com.appcloak.hide.PmDisableStrategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = PreferencesStore(application)
    private val repository = AppRepository(application)
    private val pmStrategy = PmDisableStrategy()
    private val dbStrategy = LauncherDbStrategy { activeConfig.value }

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val visibleApps: StateFlow<List<AppInfo>> = combine(apps, search) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _hiddenRecords = MutableStateFlow<List<HiddenRecord>>(store.hiddenRecords())
    val hiddenRecords: StateFlow<List<HiddenRecord>> = _hiddenRecords.asStateFlow()

    val hiddenPackages: StateFlow<Set<String>> = hiddenRecords
        .map { records -> records.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _mode = MutableStateFlow(store.mode)
    val mode: StateFlow<HideMode> = _mode.asStateFlow()

    private val _configs = MutableStateFlow(store.configs())
    val configs: StateFlow<List<LauncherDbConfig>> = _configs.asStateFlow()

    private val _activeConfigId = MutableStateFlow(store.activeConfigId)
    val activeConfigId: StateFlow<String?> = _activeConfigId.asStateFlow()

    val activeConfig: StateFlow<LauncherDbConfig?> = combine(configs, activeConfigId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _rootReady = MutableStateFlow(false)
    val rootReady: StateFlow<Boolean> = _rootReady.asStateFlow()

    private val _sqliteAvailable = MutableStateFlow(false)
    val sqliteAvailable: StateFlow<Boolean> = _sqliteAvailable.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(store.biometricEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages

    init {
        refresh()
    }

    private fun strategyFor(target: HideMode): HideStrategy = when (target) {
        HideMode.PM_DISABLE -> pmStrategy
        HideMode.LAUNCHER_DB -> dbStrategy
    }

    /** 重新读取应用列表、root 状态，并按当前策略校准隐藏记录。 */
    fun refresh() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val loaded = repository.loadApps()
                _apps.value = loaded

                val root = RootManager.checkRoot()
                _rootReady.value = root

                if (root) {
                    val current = mode.value
                    val systemHidden = try {
                        strategyFor(current).listHidden()
                    } catch (e: Exception) {
                        _messages.tryEmit("读取隐藏状态失败：${e.message}")
                        emptySet()
                    }
                    val all = store.hiddenRecords().toMutableList()
                    all.removeAll { it.mode == current }
                    val canQuery = current != HideMode.LAUNCHER_DB || activeConfig.value != null
                    if (canQuery) {
                        systemHidden.forEach { pkg ->
                            all.add(
                                HiddenRecord(
                                    packageName = pkg,
                                    label = loaded.find { it.packageName == pkg }?.label
                                        ?: repository.labelOf(pkg),
                                    mode = current,
                                ),
                            )
                        }
                    }
                    store.saveHiddenRecords(all)
                    _hiddenRecords.value = all

                    _sqliteAvailable.value = if (current == HideMode.LAUNCHER_DB) {
                        dbStrategy.isSqlite3Available()
                    } else {
                        false
                    }
                } else {
                    _hiddenRecords.value = store.hiddenRecords()
                }
            } catch (e: Exception) {
                _messages.tryEmit("刷新失败：${e.message}")
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun hideInternal(packageName: String): Result<HiddenRecord> {
        val current = mode.value
        if (current == HideMode.LAUNCHER_DB && activeConfig.value == null) {
            return Result.failure(IllegalStateException("请先配置桌面数据库"))
        }
        val result = strategyFor(current).hide(packageName)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        return Result.success(
            HiddenRecord(
                packageName = packageName,
                label = _apps.value.find { it.packageName == packageName }?.label
                    ?: repository.labelOf(packageName),
                mode = current,
            ),
        )
    }

    private fun persistAdd(record: HiddenRecord) {
        val updated = store.hiddenRecords().filterNot { it.packageName == record.packageName } + record
        store.saveHiddenRecords(updated)
        _hiddenRecords.value = updated
    }

    private fun persistRemove(packageName: String) {
        val updated = store.hiddenRecords().filterNot { it.packageName == packageName }
        store.saveHiddenRecords(updated)
        _hiddenRecords.value = updated
    }

    fun hide(packageName: String) {
        if (RootManager.validatePackage(packageName) == null) {
            _messages.tryEmit("非法包名")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val result = hideInternal(packageName)
            _busy.value = false
            result.onSuccess { record ->
                persistAdd(record)
                _messages.tryEmit("已隐藏 ${record.label}")
            }.onFailure { error ->
                _messages.tryEmit("操作失败：${error.message}")
            }
        }
    }

    fun hideSelected() {
        viewModelScope.launch {
            val targets = _selected.value.toList()
            if (targets.isEmpty()) return@launch
            _busy.value = true
            var succeeded = 0
            val failures = mutableListOf<String>()
            targets.forEach { pkg ->
                hideInternal(pkg).onSuccess {
                    persistAdd(it)
                    succeeded++
                }.onFailure { error ->
                    failures += "${repository.labelOf(pkg)}: ${error.message}"
                }
            }
            _selected.value = emptySet()
            _busy.value = false
            if (succeeded > 0) _messages.tryEmit("已隐藏 $succeeded 个应用")
            failures.take(3).forEach { _messages.tryEmit("失败：$it") }
        }
    }

    fun unhide(record: HiddenRecord) {
        viewModelScope.launch {
            _busy.value = true
            val result = strategyFor(record.mode).unhide(record.packageName)
            _busy.value = false
            result.onSuccess {
                persistRemove(record.packageName)
                _messages.tryEmit("已恢复 ${record.label}")
            }.onFailure { error ->
                _messages.tryEmit("操作失败：${error.message}")
            }
        }
    }

    fun restoreAll() {
        viewModelScope.launch {
            _busy.value = true
            var succeeded = 0
            store.hiddenRecords().forEach { record ->
                strategyFor(record.mode).unhide(record.packageName).onSuccess {
                    persistRemove(record.packageName)
                    succeeded++
                }
            }
            _busy.value = false
            _messages.tryEmit("已恢复 $succeeded 个应用")
        }
    }

    fun setMode(newMode: HideMode) {
        store.mode = newMode
        _mode.value = newMode
        _selected.value = emptySet()
        refresh()
    }

    fun setSearch(query: String) {
        _search.value = query
    }

    fun toggleSelect(packageName: String) {
        _selected.value = if (packageName in _selected.value) {
            _selected.value - packageName
        } else {
            _selected.value + packageName
        }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        store.biometricEnabled = enabled
        _biometricEnabled.value = enabled
    }

    fun saveConfig(config: LauncherDbConfig) {
        val list = store.configs().filterNot { it.id == config.id } + config
        store.saveConfigs(list)
        _configs.value = list
        if (_activeConfigId.value == null) setActiveConfig(config.id)
    }

    fun deleteConfig(id: String) {
        val list = store.configs().filterNot { it.id == id }
        store.saveConfigs(list)
        _configs.value = list
        if (_activeConfigId.value == id) {
            val next = list.firstOrNull()?.id
            store.activeConfigId = next
            _activeConfigId.value = next
            refresh()
        }
    }

    fun setActiveConfig(id: String) {
        store.activeConfigId = id
        _activeConfigId.value = id
        refresh()
    }

    suspend fun testConfig(config: LauncherDbConfig): Result<Int> = dbStrategy.test(config)
}
