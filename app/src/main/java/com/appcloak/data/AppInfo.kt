package com.appcloak.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean,
    val hasLauncher: Boolean,
)
