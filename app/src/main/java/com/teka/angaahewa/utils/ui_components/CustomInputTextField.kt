package com.teka.angaahewa.utils.ui_components


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teka.angaahewa.ui.theme.DarkGray
import com.teka.angaahewa.ui.theme.TextSizeMedium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.text.isNullOrEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomInputTextField2(
    modifier: Modifier = Modifier,
    labelText: String = "",
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    value: String,
    errorMessage: String? = null,
    maxLines: Int = 1,
    editable: Boolean = true,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onValidate: ((String) -> String?)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    shape: CornerBasedShape = MaterialTheme.shapes.small,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isOptional: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    debounceDelay: Long = 600L,
    enableRealTimeValidation: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState()
    var validationJob = remember { mutableStateOf<Job?>(null) }
    var currentError by remember { mutableStateOf<String?>(null) }

    // Use external error if provided, otherwise use internal validation error
    val displayError = errorMessage ?: currentError

    Column(
        modifier = modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minWidth = 56.dp),
            label = {
                Row {
                    Text(
                        text = "$labelText : ",
                        fontSize = TextSizeMedium,
                        color = DarkGray
                    )
                    if (!isOptional) {
                        Text(
                            text = "*",
                            color = MaterialTheme.colorScheme.error,
                            style = textStyle.copy(fontSize = textStyle.fontSize * 0.75)
                        )
                    }
                }
            },
            value = value,
            onValueChange = { newValue ->
                // Immediately call the onValueChange to update state
                onValueChange(newValue)

                // Handle validation if enabled and validation function is provided
                if (enableRealTimeValidation && onValidate != null) {
                    validationJob.value?.cancel()
                    validationJob.value = CoroutineScope(Dispatchers.Main).launch {
                        delay(debounceDelay)
                        try {
                            currentError = onValidate(newValue)
                        } catch (e: Exception) {
                            Timber.e("Validation error: ${e.message}")
                            currentError = "Validation failed"
                        }
                    }
                }
            },
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            textStyle = textStyle,
            shape = shape,
            maxLines = maxLines,
            singleLine = maxLines == 1,
            keyboardOptions = keyboardOptions,
            readOnly = !editable,
            enabled = enabled,
            isError = !displayError.isNullOrEmpty(),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                unfocusedIndicatorColor = DarkGray,
                disabledContainerColor = Color.Transparent,
            )
        )

        if (!displayError.isNullOrEmpty()) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = displayError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
            )
        }
    }
}





