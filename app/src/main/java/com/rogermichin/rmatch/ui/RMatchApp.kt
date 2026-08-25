package com.rogermichin.rmatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rogermichin.rmatch.ui.screens.AnalyticsScreen
import com.rogermichin.rmatch.ui.screens.BroadcastScreen
import com.rogermichin.rmatch.ui.screens.LeagueDetailsScreen
import com.rogermichin.rmatch.ui.screens.LeaguesScreen
import com.rogermichin.rmatch.ui.screens.MatchDetailsScreen
import com.rogermichin.rmatch.ui.screens.MatchesScreen
import com.rogermichin.rmatch.ui.screens.OnboardingScreen
import com.rogermichin.rmatch.ui.screens.SettingsScreen
import com.rogermichin.rmatch.ui.screens.TeamScreen

private data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RMatchApp(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (!state.apiKeyPresent) {
        OnboardingScreen(state.onboardingBusy, state.onboardingError, viewModel::verifyAndSaveApiKey)
        return
    }
    val navController = rememberNavController()
    val items = listOf(
        NavItem("matches", "Матчи", Icons.Default.SportsSoccer),
        NavItem("leagues", "Лиги", Icons.Default.Public),
        NavItem("analytics", "Аналитика", Icons.Default.Analytics),
        NavItem("broadcast", "Эфир", Icons.Default.LiveTv),
        NavItem("settings", "Настройки", Icons.Default.Settings),
    )
    Scaffold(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = {
                Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(2f.value))) {
                    Text("R-Match")
                    Text("Roger&Michin Studio")
                }
            })
        },
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("matches") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = "matches", modifier = androidx.compose.ui.Modifier.padding(innerPadding)) {
            composable("matches") { MatchesScreen(state, viewModel::refreshDashboard, { match -> viewModel.selectMatch(match.fixtureId, match.leagueId, match.season); navController.navigate("match/${match.fixtureId}/${match.leagueId}/${match.season}") }, viewModel::saveCountryFilter, viewModel::saveLeagueFilter) }
            composable("leagues") { LeaguesScreen(state, viewModel::refreshDashboard, { league -> viewModel.selectLeague(league.id, league.season); navController.navigate("league/${league.id}/${league.season}") }) }
            composable("analytics") { AnalyticsScreen(state, viewModel::refreshDashboard, { match -> viewModel.selectMatch(match.fixtureId, match.leagueId, match.season); navController.navigate("match/${match.fixtureId}/${match.leagueId}/${match.season}") }) }
            composable("broadcast") { BroadcastScreen() }
            composable("settings") { SettingsScreen(state, { viewModel.refreshDashboard(true) }, viewModel::verifyAndSaveApiKey, viewModel::deleteApiKeyAndCache, viewModel::clearCache) }
            composable("league/{leagueId}/{season}", arguments = listOf(navArgument("leagueId") { type = NavType.IntType }, navArgument("season") { type = NavType.IntType })) {
                LeagueDetailsScreen(state.selectedLeague, { navController.popBackStack() }, { match -> viewModel.selectMatch(match.fixtureId, match.leagueId, match.season); navController.navigate("match/${match.fixtureId}/${match.leagueId}/${match.season}") }, { teamId, leagueId, season -> viewModel.selectTeam(teamId, leagueId, season); navController.navigate("team/$teamId/$leagueId/$season") })
            }
            composable("match/{fixtureId}/{leagueId}/{season}", arguments = listOf(navArgument("fixtureId") { type = NavType.IntType }, navArgument("leagueId") { type = NavType.IntType }, navArgument("season") { type = NavType.IntType })) {
                MatchDetailsScreen(state.selectedMatch, { navController.popBackStack() }, { teamId, leagueId, season -> viewModel.selectTeam(teamId, leagueId, season); navController.navigate("team/$teamId/$leagueId/$season") })
            }
            composable("team/{teamId}/{leagueId}/{season}", arguments = listOf(navArgument("teamId") { type = NavType.IntType }, navArgument("leagueId") { type = NavType.IntType }, navArgument("season") { type = NavType.IntType })) { TeamScreen(state.selectedTeam, { navController.popBackStack() }) }
        }
    }
}
