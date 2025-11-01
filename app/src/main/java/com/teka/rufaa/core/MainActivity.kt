package com.teka.rufaa.core

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.teka.rufaa.core.navigation.RootNavGraph
import com.teka.rufaa.data_layer.DataStoreRepository
import com.teka.rufaa.modules.auth_module.AuthViewModel
import com.teka.rufaa.ui.theme.ChaiTrakTheme
import com.teka.rufaa.utils.composition_locals.DialogController
import com.teka.rufaa.utils.composition_locals.LocalDialogController
import com.teka.rufaa.utils.composition_locals.UserState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.getValue


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel by viewModels<AuthViewModel>()
    private lateinit var dataStoreRepository: DataStoreRepository
    private val dialogController = DialogController()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        dataStoreRepository = DataStoreRepository(context = applicationContext)

        val splashScreen = installSplashScreen()
        authViewModel.startDestination.value?.let { Timber.tag("TAG3").d(it) }

        setContent {
            CompositionLocalProvider(
                UserState provides authViewModel,
                LocalDialogController provides dialogController
            ) {
                ChaiTrakTheme {
                    val systemUiController = rememberSystemUiController()
                    val useDarkIcons = !isSystemInDarkTheme()

                    LaunchedEffect(systemUiController, useDarkIcons) {
                        systemUiController.setSystemBarsColor(
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            darkIcons = useDarkIcons
                        )
                    }

                    Box(
                        modifier = Modifier.imePadding()
                    ) {
                        var startDestination = authViewModel.startDestination.collectAsState().value
                        splashScreen.setKeepOnScreenCondition { startDestination.isNullOrEmpty() }

                        startDestination?.let { startDestination ->
                            RootNavGraph(
                                navController = rememberNavController(),
                                startDestination = startDestination,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun enableEdgeToEdge2() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

}

