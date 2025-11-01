package com.teka.rufaa.utils.composition_locals

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

val LocalDialogController = compositionLocalOf<DialogController> {
    error("DialogController not provided")
}

class DialogController {
    var showDialog = mutableStateOf(false)

    fun triggerDialog(show: Boolean) {
        showDialog.value = show
    }
}
