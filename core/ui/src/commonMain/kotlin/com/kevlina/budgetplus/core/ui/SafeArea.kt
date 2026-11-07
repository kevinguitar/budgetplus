package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 *  The area that system UI and display cutouts may cover, on every side.
 *
 *  Unlike [androidx.compose.foundation.layout.safeDrawing], this excludes the IME, so bars and
 *  FABs don't jump above the software keyboard.
 *
 *  The horizontal sides matter on devices where the safe area isn't symmetric, e.g. the iPhone
 *  Duo reserves an 84pt column on one side for the status bar and the camera, landscape iPhones
 *  have the Dynamic Island on one side, and Android devices can have side cutouts or a 3-button
 *  navigation bar on the side.
 */
val WindowInsets.Companion.safeArea: WindowInsets
    @Composable get() = systemBars.union(displayCutout)

/**
 *  The horizontal part of [safeArea]. Apply it to the content of a screen (below the [TopBar]),
 *  so backgrounds can still be drawn edge to edge while the foreground stays visible.
 */
val WindowInsets.Companion.safeHorizontal: WindowInsets
    @Composable get() = safeArea.only(WindowInsetsSides.Horizontal)
