package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TailGunnerDarkScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = CockpitBg,
    secondary = GunmetalSteel,
    onSecondary = ParchmentInk,
    tertiary = AlertRed,
    onTertiary = ParchmentInk,
    background = CockpitBg,
    onBackground = ParchmentInk,
    surface = PanelDark,
    onSurface = ParchmentInk,
    surfaceVariant = PanelPlate,
    onSurfaceVariant = DimSilver,
    outline = PanelBorder,
    error = AlertCrimson,
    onError = ParchmentInk
)

@Composable
fun TailGunnerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TailGunnerDarkScheme,
        typography = TailGunnerTypography,
        content = content
    )
}
