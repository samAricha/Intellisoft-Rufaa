package com.teka.rufaa.core.navigation

const val ROOT_GRAPH_ROUTE = "root_graph_route"
const val AUTH_GRAPH_ROUTE = "auth_graph_route"
const val MAIN_GRAPH_ROUTE = "main_graph_route"
const val To_MAIN_GRAPH_ROUTE = "to_main_graph_route"


sealed class AppScreens(val route: String, val title: String? = null) {


    //auth screens
    object LoginScreen : AppScreens(route = "login_screen")

    object HomeScreen : AppScreens(route = "home_screen")

    object PatientsListScreen : AppScreens(route = "patients_list_screen")

}