package com.teka.angaahewa.utils.composition_locals

import android.annotation.SuppressLint
import androidx.compose.runtime.compositionLocalOf
import com.teka.angaahewa.modules.auth_module.AuthViewModel


@SuppressLint("CompositionLocalNaming")
val UserState = compositionLocalOf<AuthViewModel> { error("User State Context Not Found!") }