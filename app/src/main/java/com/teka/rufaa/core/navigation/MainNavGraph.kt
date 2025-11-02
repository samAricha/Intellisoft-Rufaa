package com.teka.rufaa.core.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teka.rufaa.modules.auth_module.login.LoginScreen
import com.teka.rufaa.modules.home.HomeScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController(),
) {

    NavHost(
        navController = navController,
        startDestination = AppScreens.HomeScreen.route,
        route = MAIN_GRAPH_ROUTE
    ) {

        composable(
            route = AppScreens.HomeScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
            content = {
                HomeScreen(navController = navController)
            }

        )



    }
}