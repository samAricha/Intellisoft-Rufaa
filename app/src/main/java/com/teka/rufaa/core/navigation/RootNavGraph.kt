package com.teka.rufaa.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.teka.rufaa.core.MainAppScreen

@Composable
fun RootNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        route = ROOT_GRAPH_ROUTE
    ) {
        authNavGraph(navController = navController)

        composable(route = To_MAIN_GRAPH_ROUTE) {
            MainAppScreen()
        }

    }
}