package com.nuevoso.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.nuevoso.launcher.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val HankenGrotesk: FontFamily = FontFamily(
    Font(GoogleFont("Hanken Grotesk"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Hanken Grotesk"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Hanken Grotesk"), provider, weight = FontWeight.SemiBold),
    Font(GoogleFont("Hanken Grotesk"), provider, weight = FontWeight.Bold),
)

val Newsreader: FontFamily = FontFamily(
    Font(GoogleFont("Newsreader"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Newsreader"), provider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(GoogleFont("Newsreader"), provider, weight = FontWeight.SemiBold),
)

val SplineSansMono: FontFamily = FontFamily(
    Font(GoogleFont("Spline Sans Mono"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Spline Sans Mono"), provider, weight = FontWeight.Medium),
)

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Bold,     fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Bold,     fontSize = 36.sp, lineHeight = 44.sp),

    headlineLarge  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),

    titleLarge  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    bodyLarge  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall  = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    labelLarge  = TextStyle(fontFamily = SplineSansMono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = SplineSansMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = SplineSansMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
