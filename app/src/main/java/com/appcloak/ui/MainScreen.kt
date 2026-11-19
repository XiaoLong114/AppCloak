package com.appcloak.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcloak.R
import com.appcloak.data.HideMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, onLock: () -> Unit) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val hiddenRecords by vm.hiddenRecords.collectAsStateWithLifecycle()
    val message by vm.messages.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmHideSelected by remember { mutableStateOf(false) }
    var confirmRestoreAll by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (tab) {
                            0 -> stringResource(R.string.tab_apps)
                            1 -> stringResource(R.string.tab_hidden)
                            else -> stringResource(R.string.tab_settings)
                        },
                    )
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry))
                    }
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lock_now))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_apps)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_hidden)) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                )
            }
        },
        floatingActionButton = {
            when (tab) {
                0 -> if (selected.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { confirmHideSelected = true },
                        icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                        text = { Text(stringResource(R.string.hide_selected, selected.size)) },
                    )
                }

                1 -> if (hiddenRecords.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { confirmRestoreAll = true },
                        icon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        text = { Text(stringResource(R.string.restore_all)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                when (tab) {
                    0 -> AppListScreen(vm)
                    1 -> HiddenScreen(vm)
                    2 -> SettingsScreen(vm, onLock = onLock)
                }
            }
        }
    }

    if (confirmHideSelected) {
        AlertDialog(
            onDismissRequest = { confirmHideSelected = false },
            title = { Text(stringResource(R.string.hide_selected, selected.size)) },
            text = {
                Text(
                    if (mode == HideMode.PM_DISABLE) {
                        stringResource(R.string.confirm_hide_selected_pm, selected.size)
                    } else {
                        stringResource(R.string.confirm_hide_selected_db, selected.size)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.hideSelected()
                    confirmHideSelected = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmHideSelected = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (confirmRestoreAll) {
        AlertDialog(
            onDismissRequest = { confirmRestoreAll = false },
            title = { Text(stringResource(R.string.restore_all)) },
            text = { Text(stringResource(R.string.confirm_restore_all)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.restoreAll()
                    confirmRestoreAll = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
