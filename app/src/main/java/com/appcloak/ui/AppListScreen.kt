package com.appcloak.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcloak.R
import com.appcloak.data.AppInfo

@Composable
fun AppListScreen(vm: MainViewModel) {
    val apps by vm.visibleApps.collectAsStateWithLifecycle()
    val hidden by vm.hiddenPackages.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val query by vm.search.collectAsStateWithLifecycle()
    val rootReady by vm.rootReady.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (!rootReady) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.root_missing),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.root_hint),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = vm::setSearch,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    isHidden = app.packageName in hidden,
                    isSelected = app.packageName in selected,
                    enabled = rootReady,
                    onSelect = { vm.toggleSelect(app.packageName) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    isHidden: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                if (app.isSystem) {
                    Text(
                        text = stringResource(R.string.system_app),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        leadingContent = {
            val bitmap = remember(app.packageName) {
                try {
                    app.icon.toBitmap(96, 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
            } else {
                Spacer(Modifier.size(44.dp))
            }
        },
        trailingContent = {
            if (isHidden) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.already_disabled)) },
                )
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    enabled = enabled,
                )
            }
        },
        modifier = Modifier.clickable(enabled = enabled && !isHidden) { onSelect() },
    )
}
