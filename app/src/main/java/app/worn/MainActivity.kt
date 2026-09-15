package app.worn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.worn.notifications.ReplacementReminders
import app.worn.service.RemovalTimerService
import app.worn.ui.WornRoot
import app.worn.ui.WornViewModel
import app.worn.ui.theme.WornTheme

class MainActivity : ComponentActivity() {
    private val viewModel: WornViewModel by viewModels {
        WornViewModel.factory((application as WornApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RemovalTimerService.sync(this)
        ReplacementReminders.sync(this)
        handleIntent(intent)
        setContent {
            WornTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(WornTheme.colors.background),
                ) {
                    WornRoot(state, viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("open_treatment", false) ||
            intent.getBooleanExtra("start_new_set", false)
        ) {
            viewModel.requestTreatmentTab()
        }
        if (intent.getBooleanExtra("start_new_set", false)) {
            viewModel.requestStartNewSet()
        }
    }
}
