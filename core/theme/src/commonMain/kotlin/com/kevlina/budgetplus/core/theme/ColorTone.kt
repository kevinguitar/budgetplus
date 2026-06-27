package com.kevlina.budgetplus.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.color_tone_azure_coast
import budgetplus.core.common.generated.resources.color_tone_barbie
import budgetplus.core.common.generated.resources.color_tone_countryside
import budgetplus.core.common.generated.resources.color_tone_customized
import budgetplus.core.common.generated.resources.color_tone_dusk
import budgetplus.core.common.generated.resources.color_tone_milk_tea
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class ColorTone {
    MilkTea, Dusk, Countryside, Barbie, AzureCoast, Customized;

    val nameRes: StringResource
        get() = when (this) {
            MilkTea -> Res.string.color_tone_milk_tea
            Dusk -> Res.string.color_tone_dusk
            Countryside -> Res.string.color_tone_countryside
            Barbie -> Res.string.color_tone_barbie
            AzureCoast -> Res.string.color_tone_azure_coast
            Customized -> Res.string.color_tone_customized
        }

    val requiresPremium: Boolean
        get() = when (this) {
            MilkTea, Dusk, Countryside -> false
            Barbie, AzureCoast, Customized -> true
        }
}

enum class ThemeColorSemantic {
    Light, LightBg, Primary, Dark
}

@Immutable
data class ThemeColors(
    val light: Color,
    val lightBg: Color,
    val primary: Color,
    val dark: Color,
) {

    companion object {

        val MilkTea = ThemeColors(
            light = Color(0xFFFFF3E0),
            lightBg = Color(0xFFF2E2CD),
            primary = Color(0xFFC1A185),
            dark = Color(0xFF7E8072)
        )

        val Dusk = ThemeColors(
            light = Color(0xFFFFFFFF),
            lightBg = Color(0xFFFCF9E3),
            primary = Color(0xFFFFAD8F),
            dark = Color(0xFF949494),
        )

        val Countryside = ThemeColors(
            light = Color(0xFFfff5f8),
            lightBg = Color(0xFFE4E4D0),
            primary = Color(0xFF94A684),
            dark = Color(0xFFb9985a)
        )

        val Barbie = ThemeColors(
            light = Color(0xFFf9fafb),
            lightBg = Color(0xFFf9e7ef),
            primary = Color(0xFFE59EBF),
            dark = Color(0xFF849FAF)
        )

        val AzureCoast = ThemeColors(
            light = Color(0xFFfff3e0),
            lightBg = Color(0xFFe8dfcc),
            primary = Color(0xFF8fadc1),
            dark = Color(0xFF777580)
        )
    }
}

val LocalAppColors = staticCompositionLocalOf {
    ThemeColors(
        light = Color.Unspecified,
        lightBg = Color.Unspecified,
        primary = Color.Unspecified,
        dark = Color.Unspecified,
    )
}