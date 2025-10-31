package com.teka.angaahewa.utils.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.teka.angaahewa.ui.theme.DarkGray
import com.teka.angaahewa.ui.theme.TextSizeMedium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber



@Composable
fun <T> CustomDropDown3(
    modifier: Modifier = Modifier,
    labelText: String = "",
    options: List<T>,
    enabled: Boolean = true,
    isOptional: Boolean = true,
    selectedValue: String = "",
    errorMessage: String? = null,
    onValueChange: (String) -> Unit,
    onOptionSelected: (T) -> Unit,// useful when you need the original object mostly complex objects
    onValidate: ((String) -> String?)? = null,
    optionTextProvider: @Composable (T) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold
    ),
    shape: CornerBasedShape = MaterialTheme.shapes.small,
    maxDropdownHeight: Dp = 280.dp,
    debounceDelay: Long = 600L,
    enableRealTimeValidation: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var validationJob = remember { mutableStateOf<Job?>(null) }
    var currentError by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current

    val displayError = errorMessage ?: currentError

    LaunchedEffect(selectedValue) {
        if (enableRealTimeValidation && onValidate != null) {
            validationJob.value?.cancel()
            validationJob.value = CoroutineScope(Dispatchers.Main).launch {
                delay(debounceDelay)
                try {
                    currentError = onValidate(selectedValue)
                } catch (e: Exception) {
                    Timber.e("Validation error: ${e.message}")
                    currentError = "Validation failed"
                }
            }
        }
    }

    Column(modifier = modifier) {
        if (labelText.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
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
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = !expanded }
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size
                }
                .defaultMinSize(minWidth = 56.dp),
            shape = shape,
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (enabled) Color.Transparent else Color(0xFFF9FAFB)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    !displayError.isNullOrEmpty() -> MaterialTheme.colorScheme.error
                    expanded -> MaterialTheme.colorScheme.primary
                    else -> DarkGray
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (expanded) 2.dp else 0.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue.ifEmpty { "Select $labelText" },
                    style = textStyle.copy(
                        color = if (selectedValue.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF374151)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (enabled) DarkGray else Color(0xFF9CA3AF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(density) { textFieldSize.width.toDp() })
                .heightIn(max = maxDropdownHeight)
                .clip(shape)
                .background(Color.White),
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            options.forEachIndexed { index, selectionOption ->
                val optionText = selectionOption.toString()
                val isSelected = selectedValue == optionText

                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                optionTextProvider(selectionOption)
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onValueChange(optionText)
                        onOptionSelected(selectionOption)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        ),
                    colors = MenuDefaults.itemColors(
                        textColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF374151)
                    )
                )

                if (index < options.size - 1) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

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