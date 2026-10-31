package dev.hayya.core.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.ui.theme.CardBorderColor
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.LockscreenWakeBackground
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TextMuted
import dev.hayya.core.ui.theme.TextPrimary
import dev.hayya.core.ui.theme.TextSecondary
import dev.hayya.core.ui.theme.TrackBackground
import dev.hayya.core.ui.theme.WarmSandAccent
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PrayerWakeScreen(
    prayerName: String,
    windowExpiryText: String,
    autoSilenceSeconds: Int,
    onDismiss: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSilenceSeconds by remember { mutableIntStateOf(autoSilenceSeconds) }

    LaunchedEffect(autoSilenceSeconds) {
        remainingSilenceSeconds = autoSilenceSeconds
        while (remainingSilenceSeconds > 0) {
            delay(1000L)
            remainingSilenceSeconds--
        }
    }

    // Ambient pulsing glow behind prayer title to indicate active Adhan playback
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlowTransition")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlowAlpha"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = LockscreenWakeBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Mute button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onMute,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, CardBorderColor, CircleShape)
                        .background(TrackBackground)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VolumeOff,
                        contentDescription = "Mute Audio",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Center Hero: Pulsing Ambient Radial Glow + Massive Prayer Name
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Soft warm radial aura
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    WarmSandAccent.copy(alpha = pulseGlowAlpha * 0.45f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = prayerName.uppercase(),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 58.sp,
                        letterSpacing = 6.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = windowExpiryText,
                        style = TabularClockStyle.copy(
                            fontSize = 15.sp,
                            color = TextSecondary
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (remainingSilenceSeconds > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Auto-silencing in ${remainingSilenceSeconds}s",
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        )
                    }
                }
            }

            // Bottom Section: Slide to Dismiss
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp)
            ) {
                DismissSlider(
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * Tactile horizontal slider tracking gestures with 85% threshold haptic trigger.
 */
@Composable
private fun DismissSlider(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val thumbSize = 56.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(TrackBackground)
            .border(1.dp, CardBorderColor, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxDragPx = with(density) { (maxWidth - thumbSize - 8.dp).toPx() }

        // Animate return when released before threshold
        val animatedOffsetPx by animateFloatAsState(
            targetValue = if (isDragging) dragOffsetPx else 0f,
            animationSpec = tween(durationMillis = 250),
            label = "SliderReturn"
        )

        // Center track invitation text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SLIDE TO DISMISS",
                style = TabularClockStyle.copy(
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = TextMuted
                )
            )
        }

        // Circular draggable thumb with warm sand icon
        Box(
            modifier = Modifier
                .offset { IntOffset((if (isDragging) dragOffsetPx else animatedOffsetPx).roundToInt() + 8, 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color(0xFF1C1F24))
                .border(1.dp, WarmSandAccent.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            hasTriggeredHaptic = false
                        },
                        onDragEnd = {
                            isDragging = false
                            if (dragOffsetPx >= maxDragPx * 0.85f) {
                                onDismiss()
                            }
                            dragOffsetPx = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetPx = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (dragOffsetPx + dragAmount).coerceIn(0f, maxDragPx)
                            dragOffsetPx = newOffset

                            val progress = if (maxDragPx > 0f) newOffset / maxDragPx else 0f
                            if (progress >= 0.85f && !hasTriggeredHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasTriggeredHaptic = true
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Slide thumb",
                tint = WarmSandAccent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(name = "Prayer Wake Screen OLED Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrayerWakeScreenPreview() {
    HayyaTheme {
        PrayerWakeScreen(
            prayerName = "FAJR",
            windowExpiryText = "Window ends at Sunrise 06:28 AM",
            autoSilenceSeconds = 58,
            onDismiss = {},
            onMute = {}
        )
    }
}
