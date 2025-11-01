package com.teka.rufaa.utils.composition_locals

import android.annotation.SuppressLint
import androidx.compose.runtime.compositionLocalOf
import com.teka.rufaa.modules.auth_module.AuthViewModel


@SuppressLint("CompositionLocalNaming")
val UserState = compositionLocalOf<AuthViewModel> { error("User State Context Not Found!") }