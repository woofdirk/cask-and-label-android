package be.webweaver.apps.caskandlabel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import be.webweaver.apps.caskandlabel.R

object Routes {
    const val JOURNAL = "journal"
    const val GROUPS = "groups"
    const val SETTINGS = "settings"
    const val GROUP = "group/{id}"
    const val BOTTLE = "bottle/{id}"
    const val NEW_TASTING = "new?bottle={bottle}&group={group}"

    fun group(id: String) = "group/$id"
    fun bottle(id: String) = "bottle/$id"
    fun newTasting(bottle: String? = null, group: String? = null) =
        "new?bottle=${bottle.orEmpty()}&group=${group.orEmpty()}"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.JOURNAL, R.string.tab_journal, Icons.AutoMirrored.Outlined.MenuBook),
    Tab(Routes.GROUPS, R.string.tab_groups, Icons.Outlined.Groups),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Outlined.Settings),
)

@Composable
fun App(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    // Screens without their own Scaffold keep clear of the status and navigation bars (edge-to-edge).
    val plain = Modifier.fillMaxSize().safeDrawingPadding()
    Column(if (state is AppState.Ready) Modifier.fillMaxSize() else plain) {
        when (state) {
            AppState.NotConfigured -> Centered(stringResource(R.string.not_configured))
            AppState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            AppState.SignedOut -> AuthScreen(online)
            AppState.NeedsProfile -> ProfileSetupScreen(vm)
            is AppState.Ready -> MainScreen(vm, online)
        }
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MainScreen(vm: AppViewModel, online: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        topBar = { if (!online) Box(Modifier.statusBarsPadding()) { OfflineBanner() } },
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.JOURNAL, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(Routes.JOURNAL) {
                JournalScreen(vm, online,
                    onBottle = { nav.navigate(Routes.bottle(it)) },
                    onNewTasting = { nav.navigate(Routes.newTasting()) })
            }
            composable(Routes.GROUPS) { GroupsScreen(vm, online, onGroup = { nav.navigate(Routes.group(it)) }) }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
            composable(Routes.GROUP, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                GroupDetailScreen(vm, online, it.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onBottle = { id -> nav.navigate(Routes.bottle(id)) },
                    onNewTasting = { group -> nav.navigate(Routes.newTasting(group = group)) })
            }
            composable(Routes.BOTTLE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                BottleScreen(vm, online, it.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onNewTasting = { bottle -> nav.navigate(Routes.newTasting(bottle = bottle)) })
            }
            composable(
                Routes.NEW_TASTING,
                arguments = listOf(
                    navArgument("bottle") { type = NavType.StringType; defaultValue = "" },
                    navArgument("group") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                NewTastingScreen(vm, online,
                    initialBottleId = it.arguments?.getString("bottle").orEmpty().ifEmpty { null },
                    initialGroupId = it.arguments?.getString("group").orEmpty().ifEmpty { null },
                    onDone = { nav.popBackStack() })
            }
        }
    }
    JoinFromLink(vm)
}
