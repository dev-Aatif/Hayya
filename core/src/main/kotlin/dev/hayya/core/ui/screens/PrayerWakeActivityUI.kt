package dev.hayya.core.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.PostAdhanDua
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.LockscreenWakeBackground
import dev.hayya.core.ui.theme.SoraFontFamily
import dev.hayya.core.ui.theme.TabularClockStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerWakeScreen(
    prayerName: String,
    windowExpiryText: String,
    autoSilenceSeconds: Int,
    showPostAdhanDua: Boolean = true,
    onDismiss: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSilenceSeconds by remember { mutableIntStateOf(autoSilenceSeconds) }
    var showDuaSheet by remember { mutableStateOf(false) }

    LaunchedEffect(autoSilenceSeconds) {
        remainingSilenceSeconds = autoSilenceSeconds
        if (autoSilenceSeconds > 0) {
            while (remainingSilenceSeconds > 0) {
                delay(1000L)
                remainingSilenceSeconds--
            }
            // Pause auto-dismissal if user is actively reading the post-Adhan Dua
            while (showDuaSheet) {
                delay(1000L)
            }
            // Safety fallback: auto-dismiss screen if still open after fade-out completes
            delay(2500L)
            onDismiss()
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
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                        contentDescription = "Mute Audio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    MaterialTheme.colorScheme.primary.copy(alpha = pulseGlowAlpha * 0.45f),
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
                        fontFamily = SoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 58.sp,
                        letterSpacing = 6.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = windowExpiryText,
                        style = TabularClockStyle.copy(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )

                    if (remainingSilenceSeconds > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Auto-silencing in ${remainingSilenceSeconds}s",
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            // Bottom Section: Post-Adhan Dua button + Slide to Dismiss
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp)
            ) {
                if (showPostAdhanDua) {
                    OutlinedButton(
                        onClick = { showDuaSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .height(40.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "POST-ADHAN DUA",
                            style = TabularClockStyle.copy(
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                DismissSlider(
                    onDismiss = onDismiss
                )
            }
        }
    }

    if (showDuaSheet) {
        PostAdhanDuaBottomSheet(
            onDismiss = { showDuaSheet = false }
        )
    }
}

/**
 * Full-screen modal bottom sheet presenting the authentic Sahih al-Bukhari 614 Post-Adhan Dua.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostAdhanDuaBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Reference Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PostAdhanDua.TITLE,
                    style = TabularClockStyle.copy(
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = PostAdhanDua.REFERENCE,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Arabic Supplication Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = PostAdhanDua.ARABIC_TEXT,
                        fontSize = 20.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Transliteration
            Text(
                text = PostAdhanDua.TRANSLITERATION,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // English Translation
            Text(
                text = "“${PostAdhanDua.TRANSLATION}”",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Authentic Virtue Note
            Text(
                text = PostAdhanDua.VIRTUE,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Close Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(32.dp)),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }

        // Circular draggable thumb with warm sand icon
        Box(
            modifier = Modifier
                .offset { IntOffset((if (isDragging) dragOffsetPx else animatedOffsetPx).roundToInt() + 8, 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
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
                tint = MaterialTheme.colorScheme.primary,
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
