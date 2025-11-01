package com.teka.rufaa.utils.ui_components

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialogWidget(
    title: String = "",
    label: String = "",
    placeholder: String = "",
    initialValue: String = "",
    dialogIcon: ImageVector? = Icons.Outlined.Info,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    maxLines: Int = 1,
    isRequired: Boolean = true,
    validator: ((String) -> String?)? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf(initialValue) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun validateInput(text: String): Boolean {
        errorMessage = when {
            isRequired && text.isBlank() -> "This field is required"
            validator != null -> validator(text) ?: ""
            else -> ""
        }
        return errorMessage.isEmpty()
    }

    fun handleSubmit() {
        if (validateInput(inputText)) {
            isLoading = true
            keyboardController?.hide()
            onSubmit(inputText.trim())
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (dialogIcon != null || title.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (dialogIcon != null) {
                            Icon(
                                imageVector = dialogIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            if (title.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }

                        if (title.isNotEmpty()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        if (errorMessage.isNotEmpty()) {
                            errorMessage = ""
                        }
                    },
                    label = if (label.isNotEmpty()) {
                        { Text(text = label) }
                    } else null,
                    placeholder = if (placeholder.isNotEmpty()) {
                        { Text(text = placeholder) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        capitalization = capitalization,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleSubmit() }
                    ),
                    maxLines = maxLines,
                    singleLine = maxLines == 1,
                    isError = errorMessage.isNotEmpty(),
                    trailingIcon = if (errorMessage.isNotEmpty()) {
                        {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = { handleSubmit() },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && inputText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "Submit",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

object InputValidators {
    fun required(message: String = "This field is required"): (String) -> String? = { text ->
        if (text.isBlank()) message else null
    }

    fun minLength(length: Int, message: String? = null): (String) -> String? = { text ->
        if (text.length < length) {
            message ?: "Minimum $length characters required"
        } else null
    }

    fun maxLength(length: Int, message: String? = null): (String) -> String? = { text ->
        if (text.length > length) {
            message ?: "Maximum $length characters allowed"
        } else null
    }

    fun email(message: String = "Please enter a valid email"): (String) -> String? = { text ->
        if (text.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            message
        } else null
    }

    fun combine(vararg validators: (String) -> String?): (String) -> String? = { text ->
        validators.firstNotNullOfOrNull { it(text) }
    }
}