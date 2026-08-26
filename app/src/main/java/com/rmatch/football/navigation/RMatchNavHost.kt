package com.rmatch.football.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rmatch.football.core.di.ServiceLocator
import com.rmatch.football.feature.analytics.AnalyticsScreen
import com.rmatch.football.feature.broadcast.BroadcastScreen
import com.rmatch.football.feature.leagues.LeagueDetailScreen
import com.rmatch.football.feature.leagues.LeaguesScreen
import com.rmatch.football.feature.match_detail.MatchDetailScreen
import com.rmatch.football.feature.matches.MatchesScreen
import com.rmatch.football.feature.onboarding.OnboardingScreen
import com.rmatch.football.feature.player.PlayerScreen
import com.rmatch.football.feature.settings.SettingsScreen
import com.rmatch.football.feature.team.TeamScreen
import kotlinx.coroutines.flow.first

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.MATCHES, "Матчи", Icons.Filled.SportsSoccer),
    BottomTab(Routes.LEAGUES, "Лиги", Icons.Filled.Public),
    BottomTab(Routes.ANALYTICS, "Аналитика", Icons.Filled.Analytics),
    BottomTab(Routes.BROADCAST, "Эфир", Icons.Filled.LiveTv),
    BottomTab(Routes.SETTINGS, "Настройки", Icons.Filled.Settings)
)

@Composable
fun RMatchRoot() {
    val navController = rememberNavController()
    var canEnterApp by remember { mutableStateOf(ServiceLocator.apiKeyStorage.hasKey()) }
    var ready by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        canEnterApp = canEnterApp || ServiceLocator.settings.onboardingCompleted.first()
        ready = true
    }

    if (!ready) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in Routes.topLevel) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.MATCHES) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = "Вкладка ${tab.label}"
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (canEnterApp) Routes.MATCHES else Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onCompleted = {
                        canEnterApp = true
                        navController.navigate(Routes.MATCHES) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.MATCHES) {
                MatchesScreen(
                    onOpenMatch = { navController.navigate(Routes.matchDetail(it)) },
                    onOpenTeam = { navController.navigate(Routes.teamDetail(it)) }
                )
            }
            composable(Routes.LEAGUES) {
                LeaguesScreen(
                    onOpenLeague = { leagueId, season ->
                        navController.navigate(Routes.leagueDetail(leagueId, season))
                    }
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    onOpenMatch = { navController.navigate(Routes.matchDetail(it)) }
                )
            }
            composable(Routes.BROADCAST) { BroadcastScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onKeyRemoved = {
                        canEnterApp = false
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.MATCHES) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Routes.MATCH_DETAIL,
                arguments = listOf(navArgument("fixtureId") { type = NavType.IntType })
            ) { entry ->
                MatchDetailScreen(
                    fixtureId = entry.arguments?.getInt("fixtureId") ?: 0,
                    onBack = { navController.popBackStack() },
                    onOpenTeam = { navController.navigate(Routes.teamDetail(it)) }
                )
            }
            composable(
                route = Routes.TEAM_DETAIL,
                arguments = listOf(navArgument("teamId") { type = NavType.IntType })
            ) { entry ->
                TeamScreen(
                    teamId = entry.arguments?.getInt("teamId") ?: 0,
                    onBack = { navController.popBackStack() },
                    onOpenMatch = { navController.navigate(Routes.matchDetail(it)) },
                    onOpenPlayer = { navController.navigate(Routes.playerDetail(it)) }
                )
            }
            composable(
                route = Routes.PLAYER_DETAIL,
                arguments = listOf(navArgument("playerId") { type = NavType.IntType })
            ) { entry ->
                PlayerScreen(
                    playerId = entry.arguments?.getInt("playerId") ?: 0,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.LEAGUE_DETAIL,
                arguments = listOf(
                    navArgument("leagueId") { type = NavType.IntType },
                    navArgument("season") { type = NavType.IntType }
                )
            ) { entry ->
                LeagueDetailScreen(
                    leagueId = entry.arguments?.getInt("leagueId") ?: 0,
                    season = entry.arguments?.getInt("season") ?: 0,
                    onBack = { navController.popBackStack() },
                    onOpenMatch = { navController.navigate(Routes.matchDetail(it)) },
                    onOpenTeam = { navController.navigate(Routes.teamDetail(it)) }
                )
            }
        }
    }
}
