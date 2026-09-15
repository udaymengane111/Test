package app.worn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.worn.ui.history.HistoryScreen
import app.worn.ui.onboarding.OnboardingScreen
import app.worn.ui.reports.ReportsScreen
import app.worn.ui.settings.SettingsScreen
import app.worn.ui.theme.WornTheme
import app.worn.ui.today.TodayScreen
import app.worn.ui.treatment.TreatmentScreen

private enum class Tab { Today, History, Treatment }

@Composable
fun WornRoot(state: TodayUiState, vm: WornViewModel) {
    val colors = WornTheme.colors
    if (!state.ready) return
    if (!state.onboardingComplete) {
        OnboardingScreen { hours, tea, lunch, snack, set ->
            vm.completeOnboarding(hours, tea, lunch, snack, set)
        }
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var reports by rememberSaveable { mutableStateOf(false) }
    val openTreatment by vm.openTreatment.collectAsStateWithLifecycle()
    val startNew by vm.startNewSet.collectAsStateWithLifecycle()
    LaunchedEffect(openTreatment) {
        if (openTreatment) {
            tab = Tab.Treatment
            vm.consumeTreatmentTab()
        }
    }

    if (settings) {
        SettingsScreen(state, vm) { settings = false }
        return
    }
    if (reports) {
        ReportsScreen(state, vm) { reports = false }
        return
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = colors.background, tonalElevation = 0.dp) {
                NavItem(tab == Tab.Today, "Today", Icons.Outlined.Schedule) { tab = Tab.Today }
                NavItem(tab == Tab.History, "History", Icons.Outlined.CalendarMonth) { tab = Tab.History }
                NavItem(tab == Tab.Treatment, "Treatment", Icons.Outlined.Layers) { tab = Tab.Treatment }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).background(colors.background)) {
            when (tab) {
                Tab.Today -> TodayScreen(state, vm)
                Tab.History -> HistoryScreen(
                    state = state,
                    onOpenDay = { date ->
                        vm.selectDate(date)
                        tab = Tab.Today
                    },
                    onReports = { reports = true },
                )
                Tab.Treatment -> TreatmentScreen(
                    state = state,
                    vm = vm,
                    onSettings = { settings = true },
                    startNewRequested = startNew,
                    onStartNewConsumed = vm::consumeStartNewSet,
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = WornTheme.colors
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.text,
            selectedTextColor = colors.text,
            unselectedIconColor = colors.tertiary,
            unselectedTextColor = colors.tertiary,
            indicatorColor = Color.Transparent,
        ),
    )
}
