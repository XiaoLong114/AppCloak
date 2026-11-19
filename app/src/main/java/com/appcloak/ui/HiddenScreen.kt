package com.appcloak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcloak.R
import com.appcloak.data.HideMode

@Composable
fun HiddenScreen(vm: MainViewModel) {
    val records by vm.hiddenRecords.collectAsStateWithLifecycle()

    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = stringResource(R.string.no_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.hidden_count, records.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(records, key = { it.packageName }) { record ->
            ListItem(
                headlineContent = { Text(record.label) },
                supportingContent = {
                    Text(
                        text = "${record.packageName} · ${
                            if (record.mode == HideMode.PM_DISABLE) {
                                stringResource(R.string.mode_pm)
                            } else {
                                stringResource(R.string.mode_db)
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingContent = {
                    TextButton(onClick = { vm.unhide(record) }) {
                        Text(stringResource(R.string.unhide))
                    }
                },
            )
            HorizontalDivider()
        }
    }
}
