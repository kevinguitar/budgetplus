package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Navigation transitions for the screens in a nav display, a slide along the X axis, similar to
 * the Android 14 activity transitions.
 *
 * - Push: both screens slide a short distance towards the start, and the new screen quickly fades
 *   in on top of the current one.
 * - Pop: the opposite of push, the current screen fades out towards the end. Predictive back uses
 *   the same transition, so the gesture scrubs through it.
 * - None: for switching between top level destinations, e.g. the bottom nav tabs.
 *
 * Only the screen on top fades, and only while the screen underneath covers almost the whole
 * container. Otherwise the background would show through the semi-transparent screen, e.g. the
 * top bar color would blink.
 */
object NavTransitions {

    fun <S> AnimatedContentTransitionScope<S>.push(): ContentTransform {
        return slideIntoContainer(
            towards = SlideDirection.Start,
            initialOffset = ::slideOffset,
            animationSpec = slideSpec,
        ) + fadeIn(
            animationSpec = fadeInSpec,
        ) togetherWith slideOutOfContainer(
            towards = SlideDirection.Start,
            targetOffset = ::slideOffset,
            animationSpec = slideSpec,
        )
    }

    fun <S> AnimatedContentTransitionScope<S>.pop(): ContentTransform {
        return slideIntoContainer(
            towards = SlideDirection.End,
            initialOffset = ::slideOffset,
            animationSpec = slideSpec,
        ) togetherWith slideOutOfContainer(
            towards = SlideDirection.End,
            targetOffset = ::slideOffset,
            animationSpec = slideSpec,
        ) + fadeOut(
            animationSpec = fadeOutSpec,
        )
    }

    fun none(): ContentTransform = EnterTransition.None togetherWith ExitTransition.None

    fun crossfade(): ContentTransform = fadeIn() togetherWith fadeOut()

    private fun slideOffset(fullOffset: Int): Int = (fullOffset * OFFSET_FRACTION).toInt()

    /** How far both screens slide, as a fraction of the width. */
    private const val OFFSET_FRACTION = 0.15F

    /** Total duration of the transition, which is also the duration of the slide. */
    private const val DURATION = 300

    /**
     * How long the new screen takes to fade in when pushed. It's short, because the screen
     * underneath uncovers more of the background as it slides away.
     */
    private const val FADE_IN_DURATION = 90

    /**
     * How long the current screen takes to fade out at the end when popped. It can be longer,
     * because the previous screen covers more of the background as it slides in.
     */
    private const val FADE_OUT_DURATION = 180

    private val slideSpec = tween<IntOffset>(
        durationMillis = DURATION,
        easing = FastOutSlowInEasing,
    )
    private val fadeInSpec = tween<Float>(
        durationMillis = FADE_IN_DURATION,
        easing = LinearEasing,
    )
    private val fadeOutSpec = tween<Float>(
        durationMillis = FADE_OUT_DURATION,
        delayMillis = DURATION - FADE_OUT_DURATION,
        easing = LinearEasing,
    )
}
