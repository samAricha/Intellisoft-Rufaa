package com.teka.angaahewa.core.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teka.angaahewa.modules.chat_module.ChatScreen
import com.teka.angaahewa.modules.collections.collections_list.CollectionsScreen
import com.teka.angaahewa.modules.collections.collections_form.CollectionForm
import com.teka.angaahewa.modules.farms_module.FarmsScreen
import com.teka.angaahewa.modules.home_module.HomeScreen
import com.teka.angaahewa.modules.image_geotagging.GeotaggedImageScreen
import com.teka.angaahewa.modules.land_measurement.LandMeasuringScreenTrial
import com.teka.angaahewa.modules.project_modules.ProjectsScreen
import com.teka.angaahewa.utils.location.LocationAwareScreen


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
            route = AppScreens.ChatScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ){
            ChatScreen(navController)
        }

        composable(
            route = AppScreens.HomeScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ){
            HomeScreen(navController)
        }


        composable(
            route = AppScreens.LocationAwareScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ){
            LocationAwareScreen()
        }


        composable(
            route = AppScreens.LandMeasuringScreen.route, // Add this to your AppScreens
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ) {
            LandMeasuringScreenTrial(navController)
        }


        composable(
            route = AppScreens.FarmsScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ) {
            FarmsScreen(navController)
        }

        composable(
            route = AppScreens.ProjectsScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ) {
            ProjectsScreen(navController)
        }

        composable(
            route = AppScreens.GeotagImageScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ) {
            GeotaggedImageScreen(navController)
        }


        composable(
            route = AppScreens.CollectionsListScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ){
            CollectionsScreen(navController)
        }

        composable(
            route = AppScreens.CollectionsFormScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
        ){
            CollectionForm(navController)
        }


    }
}