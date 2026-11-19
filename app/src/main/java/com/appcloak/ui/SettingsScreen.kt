package com.appcloak.ui

import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcloak.R
import com.appcloak.data.HideMode

@Composable
fun SettingsScreen(vm: MainViewModel, onLock: () -> Unit) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val rootReady by vm.rootReady.collectAsStateWithLifecycle()
    val sqliteAvailable by vm.sqliteAvailable.collectAsStateWithLifecycle()
    val biometric by vm.biometricEnabled.collectAsStateWithLifecycle()
    val configs by vm.configs.collectAsStateWithLifecycle()
    var showConfig by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showConfig) {
        LauncherConfigScreen(vm = vm, onBack = { showConfig = false })
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionTitle(stringResource(R.string.section_general)) }

        item {
            ModeCard(
                title = stringResource(R.string.mode_pm),
                description = stringResource(R.string.mode_pm_desc),
                selected = mode == HideMode.PM_DISABLE,
                onSelect = { vm.setMode(HideMode.PM_DISABLE) },
            )
        }
        item {
            ModeCard(
                title = stringResource(R.string.mode_db),
                description = stringResource(R.string.mode_db_desc),
                selected = mode == HideMode.LAUNCHER_DB,
                onSelect = { vm.setMode(HideMode.LAUNCHER_DB) },
            )
        }

        item {
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (rootReady) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                },
                headlineContent = {
                    Text(
                        if (rootReady) stringResource(R.string.root_ok)
                        else stringResource(R.string.root_missing),
                    )
                },
            )
        }

        if (mode == HideMode.LAUNCHER_DB) {
            item {
                ListItem(
                    headlineContent = { Text("sqlite3") },
                    supportingContent = {
                        Text(
                            if (sqliteAvailable) "sqlite3 可用"
                            else "未找到 sqlite3，请通过 Magisk 模块安装",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
            }
        }

        item { HorizontalDivider() }
        item { SectionTitle(stringResource(R.string.section_general)) }

        item {
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.biometric_enable)) },
                supportingContent = {
                    Column {
                        Text(
                            text = stringResource(R.string.biometric_enable_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (biometricError) {
                            Text(
                                text = stringResource(R.string.biometric_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                trailingContent = {
                    Switch(
                        checked = biometric,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val authenticators =
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                val canAuthenticate =
                                    BiometricManager.from(context).canAuthenticate(authenticators)
                                if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                    vm.setBiometricEnabled(true)
                                    biometricError = false
                                    onLock()
                                } else {
                                    biometricError = true
                                }
                            } else {
                                vm.setBiometricEnabled(false)
                                biometricError = false
                            }
                        },
                    )
                },
            )
        }

        item { HorizontalDivider() }
        item { SectionTitle(stringResource(R.string.section_launcher)) }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.config_title)) },
                supportingContent = {
                    Text(
                        text = if (configs.isEmpty()) {
                            stringResource(R.string.no_config)
                        } else {
                            "${configs.size} 份配置"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.NavigateNext, contentDescription = null)
                },
                modifier = Modifier.clickable { showConfig = true },
            )
        }

        item { HorizontalDivider() }
        item { SectionTitle(stringResource(R.string.section_about)) }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.disclaimer_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = stringResource(R.string.disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
