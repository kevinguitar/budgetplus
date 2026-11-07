package com.kevlina.budgetplus.core.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 *  A hack to recompose iOS UI when the language is changed. The string resources aren't reloaded
 *  otherwise, Android recreates the activity instead. Wrap the UI that stays on screen while the
 *  language is changed in a `key` of this value, e.g. the settings screen and the nav rail.
 */
val languageSelectionState: MutableState<String?> = mutableStateOf(null)
