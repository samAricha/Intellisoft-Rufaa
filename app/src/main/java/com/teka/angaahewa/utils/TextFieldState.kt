package com.teka.angaahewa.utils

import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.flow.MutableStateFlow

data class TextFieldState(
    var text: String = "",
    var fieldId: String = "",
    var error: MutableStateFlow<String?> = MutableStateFlow(null),
    val keyboardType: KeyboardType = KeyboardType.Text,
    val isOptional: Boolean = false,
    val labelText: String = ""
) {
    fun validate() {
        error.value = if (!isOptional && text.isBlank()) {
            "$labelText is required"
        } else {
            null
        }
    }
}
