package com.teka.angaahewa.core.navigation

const val ROOT_GRAPH_ROUTE = "root_graph_route"
const val AUTH_GRAPH_ROUTE = "auth_graph_route"
const val MAIN_GRAPH_ROUTE = "main_graph_route"
const val To_MAIN_GRAPH_ROUTE = "to_main_graph_route"


sealed class AppScreens(val route: String, val title: String? = null) {

    object HomeScreen : AppScreens(route = "home_screen")
    object LocationAwareScreen : AppScreens(route = "location_aware_screen")
    object ChatScreen : AppScreens(route = "chat_screen")
    object LandMeasuringScreen : AppScreens(route = "land_measuring_screen")
    object FarmsScreen : AppScreens(route = "farms_screen")
    object ProjectsScreen : AppScreens(route = "projects_screen")
    object GeotagImageScreen : AppScreens(route = "geo_tag_images_screen")

    object CollectionsListScreen : AppScreens(route = "collections_list_screen")
    object CollectionsFormScreen : AppScreens(route = "collections_form_screen")

    //auth screens
    object LoginScreen : AppScreens(route = "login_screen")

}