package com.kevlina.budgetplus.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import kotlin.jvm.JvmInline

@JvmInline
value class TypographyScale(val scale: Float)

@Composable
fun Modifier.typographyScale(): Modifier = apply {
    scale(LocalTypographyScale.current.scale)
}

val LocalTypographyScale = staticCompositionLocalOf {
    TypographyScale(1.0F)
}