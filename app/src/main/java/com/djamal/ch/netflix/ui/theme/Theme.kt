package com.djamal.ch.netflix.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.darkColorScheme

@Composable
fun NetflixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NetflixRed,
            onPrimary = Color.White,
            background = NetflixBlack,
            onBackground = Color.White,
            surface = NetflixBlack,
            onSurface = Color.White,
            surfaceVariant = SurfaceGrey,
            onSurfaceVariant = TextGrey,
        ),
        typography = Typography,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            colors = SurfaceDefaults.colors(containerColor = NetflixBlack, contentColor = Color.White),
            content = { content() },
        )
    }
}
