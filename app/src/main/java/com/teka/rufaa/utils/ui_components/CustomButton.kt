package com.teka.rufaa.utils.ui_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teka.rufaa.ui.theme.PrimaryGreen
import com.teka.rufaa.ui.theme.rajdhani


@Composable
fun CustomButton(
    modifier: Modifier = Modifier.height(50.dp),
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = PrimaryGreen,
    btnText: String,
    textFontSize: TextUnit? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "Loading..."
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        colors = colors,
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = loadingText,
                    fontSize = textFontSize ?: 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = rajdhani
                )
            }
        } else {
            Text(
                text = btnText,
                fontSize = textFontSize ?: 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = rajdhani
            )
        }
    }
}