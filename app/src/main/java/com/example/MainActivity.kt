package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.model.MultiviewLayoutMode
import com.example.ui.screens.ChannelGuideScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ManualIpScreen
import com.example.ui.screens.MultiviewScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SavedLayoutsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBackground
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TabloAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        viewModel.onTriggerPip = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to enter PiP: ${e.message}")
                }
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBackground
                ) {
                    TabloAppRoot(
                        viewModel = viewModel,
                        onExitApp = { finish() }
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPipMode(isInPictureInPictureMode)
    }

    override fun onPause() {
        super.onPause()
        viewModel.playerManager.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.currentScreen.value == AppScreen.MULTIVIEW) {
            viewModel.playerManager.resumeAll()
        }
    }
}

@Composable
fun TabloAppRoot(
    viewModel: TabloAppViewModel,
    onExitApp: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val multiviewState by viewModel.multiviewState.collectAsState()
    val currentDevice by viewModel.deviceRepository.currentDevice.collectAsState()

    // Handle Fire TV Remote BACK button
    BackHandler {
        when (currentScreen) {
            AppScreen.MULTIVIEW -> {
                when {
                    multiviewState.isMultiviewBuilderOpen -> viewModel.closeMultiviewBuilder()
                    multiviewState.isStatsOverlayOpen -> viewModel.closeStatsOverlay()
                    multiviewState.isPlaybackOverlayVisible -> viewModel.togglePlaybackOverlay(false)
                    multiviewState.isChannelPickerOpen -> viewModel.closeChannelPicker()
                    multiviewState.isActionMenuOpen -> viewModel.toggleActionMenu(false)
                    multiviewState.isSaveLayoutDialogOpen -> viewModel.closeSaveLayoutDialog()
                    multiviewState.isFullScreenSingle && multiviewState.previousModeBeforeFullScreen != MultiviewLayoutMode.ONE_PANE -> {
                        // Return directly to 4-way Multiview layout on remote Back
                        viewModel.toggleFullScreenActivePane()
                    }
                    else -> viewModel.navigateTo(AppScreen.HOME)
                }
            }
            AppScreen.MANUAL_IP -> viewModel.navigateTo(AppScreen.REGISTRATION)
            AppScreen.LIBRARY,
            AppScreen.LIVE,
            AppScreen.CHANNEL_GUIDE,
            AppScreen.SAVED_LAYOUTS,
            AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.REGISTRATION -> {
                if (currentDevice != null) {
                    viewModel.navigateTo(AppScreen.HOME)
                } else {
                    onExitApp()
                }
            }
            AppScreen.HOME -> onExitApp()
        }
    }

    when (currentScreen) {
        AppScreen.LIBRARY -> LibraryScreen(viewModel = viewModel)
        AppScreen.HOME -> HomeScreen(viewModel = viewModel)
        AppScreen.LIVE -> ChannelGuideScreen(viewModel = viewModel)
        AppScreen.REGISTRATION -> RegistrationScreen(viewModel = viewModel)
        AppScreen.MANUAL_IP -> ManualIpScreen(viewModel = viewModel)
        AppScreen.MULTIVIEW -> MultiviewScreen(viewModel = viewModel)
        AppScreen.CHANNEL_GUIDE -> ChannelGuideScreen(viewModel = viewModel)
        AppScreen.SAVED_LAYOUTS -> SavedLayoutsScreen(viewModel = viewModel)
        AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
    }
}
