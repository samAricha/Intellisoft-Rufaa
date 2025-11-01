package com.teka.rufaa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.teka.rufaa.R

val quicksand = FontFamily(
    Font(R.font.quicksand_light, FontWeight.Light),
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold)
)

val roboto = FontFamily(
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
)


val rajdhani = FontFamily(
    Font(R.font.rajdhani_light, FontWeight.Light),
    Font(R.font.rajdhani_regular, FontWeight.Normal),
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
    Font(R.font.rajdhani_semi_bold, FontWeight.SemiBold),
)


private val defaultTypography = Typography()


val customTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = roboto,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    ),
    displayMedium = TextStyle(
        fontFamily = rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    displaySmall =  TextStyle(
        fontFamily = rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = rajdhani),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = rajdhani),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = rajdhani),


    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontFamily = rajdhani,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontFamily = rajdhani,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontFamily = rajdhani,
        fontSize = 10.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),


    bodyLarge = TextStyle(
        fontFamily = rajdhani,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = rajdhani,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    bodySmall = TextStyle(
        fontFamily = rajdhani,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    ),



    labelLarge = defaultTypography.labelLarge.copy(fontFamily = rajdhani),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = rajdhani),
    labelSmall = TextStyle(
        fontFamily = rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),

    )
