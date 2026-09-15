package app.worn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        setContent {
            WornTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(WornTheme.colors.background),
                ) {
                    WornRoot(state, viewModel)
                }
            }
        }
    }
}
