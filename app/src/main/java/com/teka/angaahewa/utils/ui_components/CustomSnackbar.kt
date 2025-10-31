package com.teka.angaahewa.utils.ui_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

enum class SnackbarMessageType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class SnackbarConfig(
    val message: String,
    val type: SnackbarMessageType,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = when (type) {
        SnackbarMessageType.SUCCESS -> SnackbarDuration.Short
        SnackbarMessageType.ERROR -> SnackbarDuration.Long
        SnackbarMessageType.WARNING -> SnackbarDuration.Long
        SnackbarMessageType.INFO -> SnackbarDuration.Short
    },
    val withHapticFeedback: Boolean = true
)

@Composable
fun CustomSnackbar(
    snackbarData: SnackbarData,
    messageType: SnackbarMessageType
) {
    val (backgroundColor, contentColor, icon) = when (messageType) {
        SnackbarMessageType.SUCCESS -> Triple(
            Color(0xFF4CAF50), // Green
            Color.White,
            Icons.Default.CheckCircle
        )
        SnackbarMessageType.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        SnackbarMessageType.WARNING -> Triple(
            Color(0xFFFF9800), // Orange
            Color.White,
            Icons.Default.Warning
        )
        SnackbarMessageType.INFO -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Info
        )
    }

    Snackbar(
        modifier = Modifier,
        shape = RoundedCornerShape(8.dp),
        containerColor = backgroundColor,
        contentColor = contentColor,
        action = {
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = messageType.name,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CustomSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            // Extract message type from the message (encoded as prefix)
            val message = snackbarData.visuals.message
            val (messageType, actualMessage) = when {
                message.startsWith("SUCCESS:") -> SnackbarMessageType.SUCCESS to message.removePrefix("SUCCESS:")
                message.startsWith("ERROR:") -> SnackbarMessageType.ERROR to message.removePrefix("ERROR:")
                message.startsWith("WARNING:") -> SnackbarMessageType.WARNING to message.removePrefix("WARNING:")
                message.startsWith("INFO:") -> SnackbarMessageType.INFO to message.removePrefix("INFO:")
                else -> SnackbarMessageType.INFO to message
            }

            // Create modified snackbar data with clean message
            val modifiedSnackbarData = object : SnackbarData {
                override val visuals = object : SnackbarVisuals {
                    override val message = actualMessage
                    override val actionLabel = snackbarData.visuals.actionLabel
                    override val withDismissAction = snackbarData.visuals.withDismissAction
                    override val duration = snackbarData.visuals.duration
                }
                override fun performAction() = snackbarData.performAction()
                override fun dismiss() = snackbarData.dismiss()
            }

            CustomSnackbar(
                snackbarData = modifiedSnackbarData,
                messageType = messageType
            )
        }
    )
}


// Hook for managing snackbar messages
@Composable
fun rememberSnackbarManager(): SnackbarManager {
    val hostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    return remember {
        SnackbarManager(
            hostState = hostState,
            haptic = haptic
        )
    }
}

class SnackbarManager(
    val hostState: SnackbarHostState,
    private val haptic: HapticFeedback
) {
    // Make currentMessageType a mutableStateOf so it can be observed
    private val _currentMessageType = mutableStateOf(SnackbarMessageType.INFO)
    val currentMessageType: SnackbarMessageType by _currentMessageType

    suspend fun showSnackbar(config: SnackbarConfig) {
        // Update the message type BEFORE showing the snackbar
        _currentMessageType.value = config.type

        // Perform haptic feedback if enabled
        if (config.withHapticFeedback) {
            val hapticType = when (config.type) {
                SnackbarMessageType.SUCCESS -> HapticFeedbackType.TextHandleMove
                SnackbarMessageType.ERROR -> HapticFeedbackType.LongPress
                SnackbarMessageType.WARNING -> HapticFeedbackType.LongPress
                SnackbarMessageType.INFO -> HapticFeedbackType.TextHandleMove
            }
            haptic.performHapticFeedback(hapticType)
        }

        hostState.showSnackbar(
            message = config.message,
            actionLabel = config.actionLabel,
            duration = config.duration
        )
    }

    suspend fun showSuccess(
        message: String,
        actionLabel: String? = "OK",
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.SUCCESS,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }

    suspend fun showError(
        message: String,
        actionLabel: String? = "Dismiss",
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.ERROR,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }

    suspend fun showWarning(
        message: String,
        actionLabel: String? = "OK",
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.WARNING,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }

    suspend fun showInfo(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.INFO,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }
}


// Extension functions for easier usage
@Composable
fun HandleSnackbarMessages(
    snackbarManager: SnackbarManagerWithEncoding,
    errorMessage: String?,
    successMessage: String?,
    warningMessage: String? = null,
    infoMessage: String? = null,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit,
    onClearWarning: (() -> Unit)? = null,
    onClearInfo: (() -> Unit)? = null
) {
    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarManager.showError(it)
            onClearError()
        }
    }

    // Handle success messages
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarManager.showSuccess(it)
            onClearSuccess()
        }
    }

    // Handle warning messages
    LaunchedEffect(warningMessage) {
        warningMessage?.let {
            snackbarManager.showWarning(it)
            onClearWarning?.invoke()
        }
    }

    // Handle info messages
    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            snackbarManager.showInfo(it)
            onClearInfo?.invoke()
        }
    }
}


class SnackbarManagerWithEncoding(
    val hostState: SnackbarHostState,
    private val haptic: HapticFeedback
) {
    suspend fun showSnackbar(config: SnackbarConfig) {
        // Perform haptic feedback if enabled
        if (config.withHapticFeedback) {
            val hapticType = when (config.type) {
                SnackbarMessageType.SUCCESS -> HapticFeedbackType.TextHandleMove
                SnackbarMessageType.ERROR -> HapticFeedbackType.LongPress
                SnackbarMessageType.WARNING -> HapticFeedbackType.LongPress
                SnackbarMessageType.INFO -> HapticFeedbackType.TextHandleMove
            }
            haptic.performHapticFeedback(hapticType)
        }

        // Encode message type in the message
        val encodedMessage = "${config.type.name}:${config.message}"

        hostState.showSnackbar(
            message = encodedMessage,
            actionLabel = config.actionLabel,
            duration = config.duration
        )
    }

    suspend fun showSuccess(
        message: String,
        actionLabel: String? = "OK",
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.SUCCESS,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }

    suspend fun showError(
        message: String,
        actionLabel: String? = "Dismiss",
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.ERROR,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }

    suspend fun showWarning(
        message: String,
        actionLabel: String? = "OK",
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.WARNING,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }

    suspend fun showInfo(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        showSnackbar(
            SnackbarConfig(
                message = message,
                type = SnackbarMessageType.INFO,
                actionLabel = actionLabel,
                duration = duration
            )
        )
    }
}
