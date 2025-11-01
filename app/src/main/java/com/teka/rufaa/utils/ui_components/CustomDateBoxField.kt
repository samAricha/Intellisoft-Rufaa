package com.teka.rufaa.utils.ui_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.teka.rufaa.ui.theme.DarkGray

@Composable
fun CustomDateBoxField(
    modifier: Modifier = Modifier,
    labelText: String = "Date",
    editable: Boolean = false,
    isOptional: Boolean = false,
    currentTextState: String,
    onClick: () -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
    shape: CornerBasedShape = MaterialTheme.shapes.small,
) {

    Column(
        modifier = modifier
            .clickable {
                onClick()
            }
    ) {
        CustomInputTextField2(
            labelText = labelText,
            value = currentTextState,
            onValueChange = {},
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date icon",
                    tint = DarkGray
                )
            },
            editable = editable,
            enabled = false,
            isOptional = isOptional,
        )

    }
}