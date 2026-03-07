package com.kevlina.budgetplus.core.theme

import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK
import com.kevlina.budgetplus.core.common.nav.NAV_COLORS_PATH
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SingleIn(AppScope::class)
@Inject
class ThemeManager(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val preference: Preference,
    private val authManager: AuthManager,
    private val tracker: Tracker,
) {
    private val colorToneKey = stringPreferencesKey("colorToneCache")
    private val colorToneFlow = preference.of(colorToneKey, ColorTone.serializer())

    private val customizedColorsKey = stringPreferencesKey("customizedColorsCache")
    private val customizedColorsFlow = preference.of(customizedColorsKey)

    // It represents the current theme colors in the customized theme.
    private var currentCustomColors = ThemeColors.MilkTea

    // This field isn't saved to the preference, it represents the colors that the user is currently mixing.
    private var editedCustomColors = currentCustomColors

    val colorTone: StateFlow<ColorTone> = colorToneFlow
        .filterNotNull()
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = runBlocking {
                val tone = colorToneFlow.first()
                // If the color tone is customized, block and parse the customized colors before rendering UI.
                if (tone == ColorTone.Customized) {
                    parseCustomColorsFromCache()
                }
                tone ?: ColorTone.MilkTea
            }
        )

    val themeColors: StateFlow<ThemeColors> = colorTone.mapState(::getThemeColors)

    val previewColors: StateFlow<ThemeColors?>
        field = MutableStateFlow<ThemeColors?>(null)

    val hasEditedCustomTheme: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        appScope.launch {
            parseCustomColorsFromCache()
        }
    }

    fun getThemeColors(colorTone: ColorTone): ThemeColors = when (colorTone) {
        ColorTone.MilkTea -> ThemeColors.MilkTea
        ColorTone.Dusk -> ThemeColors.Dusk
        ColorTone.Countryside -> ThemeColors.Countryside
        ColorTone.Barbie -> ThemeColors.Barbie
        ColorTone.Lavender -> ThemeColors.Lavender
        ColorTone.NemoSea -> ThemeColors.NemoSea
        ColorTone.Customized -> editedCustomColors
    }

    suspend fun setColorTone(newColorTone: ColorTone) {
        if (!authManager.isPremium.value && newColorTone.requiresPremium) {
            Logger.e { "ThemeManager: Attempting to apply a premium theme for a free user. $colorTone" }
            return
        }

        if (newColorTone == ColorTone.Customized) {
            saveCustomColors(editedCustomColors)
        }

        preference.update(colorToneKey, ColorTone.serializer(), newColorTone)
        tracker.logEvent("color_tone_changed", mapOf("tone" to newColorTone.name))
    }

    fun setPreviewColors(newPreviewColors: ThemeColors) {
        previewColors.value = newPreviewColors
    }

    fun clearPreviewColors() {
        previewColors.value = null
        hasEditedCustomTheme.value = false
        // Reset the edited custom colors
        editedCustomColors = currentCustomColors
    }

    fun onColorPicked(semantic: ThemeColorSemantic, colorHexCode: String) {
        val newColor = try {
            colorHexCode.convertHexToColor()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to parse the picked color $colorHexCode" }
            return
        }

        val newColors = when (semantic) {
            ThemeColorSemantic.Light -> editedCustomColors.copy(light = newColor)
            ThemeColorSemantic.LightBg -> editedCustomColors.copy(lightBg = newColor)
            ThemeColorSemantic.Primary -> editedCustomColors.copy(primary = newColor)
            ThemeColorSemantic.Dark -> editedCustomColors.copy(dark = newColor)
        }

        setPreviewColors(newColors)
        editedCustomColors = newColors
        tracker.logEvent("color_tone_custom_color_picked")

        // If the customized tone is not selected, write the new color hex directly.
        if (colorTone.value == ColorTone.Customized) {
            hasEditedCustomTheme.value = currentCustomColors != newColors
        } else {
            appScope.launch { saveCustomColors(newColors) }
        }
    }

    fun generateCustomColorsLink(): String {
        val colorsLink = "${APP_DEEPLINK}/${NAV_COLORS_PATH}?hex=${editedCustomColors.encode()}"
        tracker.logEvent("color_tone_link_generated")
        return colorsLink.lowercase()
    }

    /**
     *  @return if the hex is decoded successfully.
     */
    fun processHexFromLink(hexFromLink: String): Boolean {
        val sharedColors = decodeThemeColors(hexFromLink) ?: return false
        setPreviewColors(sharedColors)
        editedCustomColors = sharedColors
        hasEditedCustomTheme.value = currentCustomColors != sharedColors
        tracker.logEvent("share_colors_link_opened")
        return true
    }

    private suspend fun parseCustomColorsFromCache() {
        customizedColorsFlow.first()?.let { encodedCustomizedColors ->
            decodeThemeColors(encodedCustomizedColors)?.let {
                currentCustomColors = it
                editedCustomColors = it
            }
        }
    }

    private suspend fun saveCustomColors(newColors: ThemeColors) {
        currentCustomColors = newColors
        editedCustomColors = newColors
        hasEditedCustomTheme.value = false
        preference.update(customizedColorsKey, newColors.encode())
    }

    private fun ThemeColors.encode(): String {
        return listOf(light, lightBg, primary, dark)
            .joinToString(COLOR_HEX_CONCAT_CHAR_URL_ENCODED) { it.toHexCode() }
    }

    @Suppress("MagicNumber")
    private fun decodeThemeColors(value: String): ThemeColors? {
        val hexCodes = value.split(COLOR_HEX_CONCAT_CHAR, COLOR_HEX_CONCAT_CHAR_URL_ENCODED)
        return try {
            ThemeColors(
                light = hexCodes[0].convertHexToColor(),
                lightBg = hexCodes[1].convertHexToColor(),
                primary = hexCodes[2].convertHexToColor(),
                dark = hexCodes[3].convertHexToColor(),
            )
        } catch (e: Exception) {
            Logger.e(e) { "Failed to decode the theme colors. raw value=$value" }
            null
        }
    }

    companion object {
        private const val COLOR_HEX_CONCAT_CHAR = ";"
        private const val COLOR_HEX_CONCAT_CHAR_URL_ENCODED = "%3b"
    }
}