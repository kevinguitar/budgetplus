package com.kevlina.budgetplus.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.jvm.JvmInline

@JvmInline
value class TypographyScale(val scale: Float)

@Composable
fun Modifier.typographyScale(): Modifier = apply {
    scale(LocalTypographyScale.current.scale)
}

@Composable
fun Dp.withTypographyScale(): Dp {
    return this * LocalTypographyScale.current.scale
}

@Composable
fun TextUnit.withTypographyScale(): TextUnit {
    return if (this == TextUnit.Unspecified) {
        this
    } else {
        this * LocalTypographyScale.current.scale
    }
}

val LocalTypographyScale = staticCompositionLocalOf {
    TypographyScale(1.0F)
}