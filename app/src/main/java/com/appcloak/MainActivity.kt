package com.appcloak

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appcloak.ui.MainScreen
import com.appcloak.ui.MainViewModel
import com.appcloak.ui.UnlockScreen
import com.appcloak.ui.theme.AppCloakTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppCloakTheme {
                val vm: MainViewModel = viewModel()
                val requireUnlock by vm.biometricEnabled.collectAsStateWithLifecycle()
                var unlocked by rememberSaveable { mutableStateOf(!requireUnlock) }

                if (unlocked) {
                    MainScreen(vm = vm, onLock = { unlocked = false })
                } else {
                    UnlockScreen(onUnlocked = { unlocked = true })
                }
            }
        }
    }
}
