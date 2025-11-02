package com.teka.rufaa.core.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teka.rufaa.modules.auth_module.login.LoginScreen
import com.teka.rufaa.modules.home.HomeScreen
import com.teka.rufaa.modules.patient_details.PatientDetailScreen
import com.teka.rufaa.modules.patient_registration.PatientRegistrationScreen
import com.teka.rufaa.modules.patients_list.PatientsListScreen
import com.teka.rufaa.modules.vitals.VitalsScreen
import com.teka.rufaa.modules.vitals.general_assesment.GeneralAssessmentScreen
import com.teka.rufaa.modules.vitals.overweight_assesment.OverweightAssessmentScreen


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

        composable(
            route = AppScreens.PatientsListScreen.route,
            enterTransition = ScreenTransitions.enterTransition,
            exitTransition = ScreenTransitions.exitTransition,
            popEnterTransition = ScreenTransitions.popEnterTransition,
            popExitTransition = ScreenTransitions.popExitTransition,
            content = {
                PatientsListScreen(navigator = navController)
            }

        )

        composable("patient_detail/{patientId}") { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")?.toIntOrNull() ?: 0
            PatientDetailScreen(patientId = patientId, navigator = navController)
        }

        composable("patient_registration") {
            PatientRegistrationScreen(navigator = navController)
        }

        composable("vitals/{patientId}") { backStackEntry ->
            VitalsScreen(
                navigator = navController,
                patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            )
        }

        composable("general_assessment/{patientId}") { backStackEntry ->
            GeneralAssessmentScreen(
                navigator = navController,
                patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            )
        }

        composable("overweight_assessment/{patientId}") { backStackEntry ->
            OverweightAssessmentScreen(
                navigator = navController,
                patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            )
        }



    }
}