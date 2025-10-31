package com.teka.angaahewa.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavBackStackEntry
import com.teka.angaahewa.ui.animations.scaleIntoContainer
import com.teka.angaahewa.ui.animations.scaleOutOfContainer

@OptIn(ExperimentalAnimationApi::class)
object ScreenTransitions {
    val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) = {
        scaleIntoContainer()
    }
    val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
        scaleOutOfContainer(direction = AnimatedContentTransitionScope.SlideDirection.Right)
    }
    val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) = {
        scaleIntoContainer(direction = AnimatedContentTransitionScope.SlideDirection.Left)
    }
    val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
        scaleOutOfContainer()
    }
}
