package dev.project.app3.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontImpl
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import dev.project.app3.R

// 1. Локальний шрифт у стилі "Історії іграшок" (res/font/toy_rubik_bubbles.ttf)
val ToyStoryFontFamily = FontFamily(
    Font(R.font.toy_rubik_bubbles, FontWeight.Normal),
    Font(R.font.toy_rubik_bubbles, FontWeight.Bold),
    Font(R.font.toy_rubik_bubbles, FontWeight.ExtraBold)
)

// 2. Шрифт із Google Fonts через Downloadable Fonts Provider (Comfortaa)
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val comfortaaGoogleFont = GoogleFont("Comfortaa")

val GoogleBodyFontFamily = FontFamily(
    GoogleFontImpl(googleFont = comfortaaGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    GoogleFontImpl(googleFont = comfortaaGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    GoogleFontImpl(googleFont = comfortaaGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    GoogleFontImpl(googleFont = comfortaaGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = ToyStoryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ToyStoryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ToyStoryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ToyStoryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleBodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleBodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp
    )
)
