package com.appcloak.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcloak.R
import com.appcloak.data.LauncherDbConfig
import kotlinx.coroutines.launch

/** 桌面数据库配置的列表与编辑页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherConfigScreen(vm: MainViewModel, onBack: () -> Unit) {
    val configs by vm.configs.collectAsStateWithLifecycle()
    val activeId by vm.activeConfigId.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LauncherDbConfig?>(null) }
    var creating by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (creating || editing != null) {
        ConfigEditor(
            initial = editing ?: LauncherDbConfig.sample().copy(id = LauncherDbConfig.newId()),
            testResult = testResult,
            onBack = {
                creating = false
                editing = null
                testResult = null
            },
            onSave = { config ->
                vm.saveConfig(config)
                creating = false
                editing = null
                testResult = null
            },
            onTest = { config ->
                scope.launch {
                    val result = vm.testConfig(config)
                    testResult = result.fold(
                        onSuccess = { count -> context.getString(R.string.config_test_ok, count) },
                        onFailure = { error -> "失败：${error.message}" },
                    )
                }
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                creating = true
                testResult = null
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_config))
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = stringResource(R.string.launcher_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )

            if (configs.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_config),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(configs, key = { it.id }) { config ->
                    ListItem(
                        headlineContent = { Text(config.name) },
                        supportingContent = {
                            Text(
                                text = "${config.launcherPackage} · ${config.dbPath}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                if (activeId == config.id) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(stringResource(R.string.current)) },
                                    )
                                } else {
                                    TextButton(onClick = { vm.setActiveConfig(config.id) }) {
                                        Text(stringResource(R.string.enable))
                                    }
                                }
                                IconButton(onClick = {
                                    editing = config
                                    testResult = null
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_config))
                                }
                                IconButton(onClick = { vm.deleteConfig(config.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigEditor(
    initial: LauncherDbConfig,
    testResult: String?,
    onBack: () -> Unit,
    onSave: (LauncherDbConfig) -> Unit,
    onTest: (LauncherDbConfig) -> Unit,
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var launcherPackage by rememberSaveable(initial.id) { mutableStateOf(initial.launcherPackage) }
    var dbPath by rememberSaveable(initial.id) { mutableStateOf(initial.dbPath) }
    var querySql by rememberSaveable(initial.id) { mutableStateOf(initial.querySql) }
    var hideSql by rememberSaveable(initial.id) { mutableStateOf(initial.hideSql) }
    var unhideSql by rememberSaveable(initial.id) { mutableStateOf(initial.unhideSql) }

    val current = LauncherDbConfig(
        id = initial.id,
        name = name,
        launcherPackage = launcherPackage,
        dbPath = dbPath,
        querySql = querySql,
        hideSql = hideSql,
        unhideSql = unhideSql,
    )
    val valid = name.isNotBlank() && launcherPackage.isNotBlank() && dbPath.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.config_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = launcherPackage,
                onValueChange = { launcherPackage = it },
                label = { Text(stringResource(R.string.config_launcher_pkg)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dbPath,
                onValueChange = { dbPath = it },
                label = { Text(stringResource(R.string.config_db_path)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = querySql,
                onValueChange = { querySql = it },
                label = { Text(stringResource(R.string.config_query_sql)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = hideSql,
                onValueChange = { hideSql = it },
                label = { Text(stringResource(R.string.config_hide_sql)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = unhideSql,
                onValueChange = { unhideSql = it },
                label = { Text(stringResource(R.string.config_unhide_sql)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Button(onClick = { onTest(current) }, enabled = valid) {
                    Text(stringResource(R.string.config_test))
                }
                TextButton(
                    onClick = { onSave(current) },
                    enabled = valid,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.config_save))
                }
            }

            if (testResult != null) {
                Text(
                    text = testResult!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
