package dev.hayya.core.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.tasbih.DhikrPhase
import dev.hayya.core.domain.tasbih.TasbihMilestoneHelper
import dev.hayya.core.domain.tasbih.TasbihTargetMode
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import kotlinx.coroutines.launch

/**
 * Pure OLED Pitch Black (`#000000`) background color for true pixel deactivation.
 */
private val PitchBlackOled = Color(0xFF000000)

/**
 * Full-screen minimalist OLED dark Tasbih / Dhikr counter where the entire screen
 * acts as an unmissable tactile touch target.
 *
 * Requirements satisfied:
 * 1. Full-screen touch target: Any tap across the canvas registers an instant increment.
 * 2. Instant haptic click: Uses system [HapticFeedbackConstants.CLOCK_TICK] on every tap.
 * 3. Distinct vibration pulse at 33, 66, and 99/100 intervals via waveform vibration.
 * 4. Preserves dark luxury typography with tabular figures and authentic Sunnah Dhikr phases.
 */
@Composable
fun TasbihScreen(
    count: Int,
    targetMode: TasbihTargetMode,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onSelectMode: (TasbihTargetMode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var showResetDialog by remember { mutableStateOf(false) }

    // Tap pulse animation scale
    val tapScale = remember { Animatable(1f) }

    // Pulsing aura for "Tap anywhere" prompt
    val infiniteTransition = rememberInfiniteTransition(label = "PromptAura")
    val promptAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PromptPulse"
    )

    // Current phase information based on count & mode
    val currentPhase: DhikrPhase = remember(count, targetMode) {
        TasbihMilestoneHelper.getPhaseForCount(count, targetMode)
    }

    val progressFraction: Float = remember(count, targetMode) {
        TasbihMilestoneHelper.calculateProgress(count, targetMode)
    }

    val lapNumber: Int = remember(count, targetMode) {
        TasbihMilestoneHelper.calculateLap(count, targetMode)
    }

    val nextMilestone: Int = remember(count, targetMode) {
        TasbihMilestoneHelper.getNextMilestone(count, targetMode)
    }

    // Handles tap events: triggers instant CLOCK_TICK, checks milestone for heavy pulse, increments
    val handleTap = {
        val nextCount = count + 1
        if (TasbihMilestoneHelper.isMilestone(nextCount, targetMode)) {
            triggerMilestoneVibration(context)
        } else {
            triggerClockTickHaptic(view, context)
        }

        scope.launch {
            tapScale.animateTo(0.96f, animationSpec = tween(durationMillis = 40))
            tapScale.animateTo(1f, animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
        }

        onIncrement()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(targetMode, count) {
                detectTapGestures(
                    onTap = { handleTap() }
                )
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // TOP BAR: Back Navigation, Lap Badge, and Controls
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Dashboard",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Lap / Cycle Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (targetMode == TasbihTargetMode.FREE) {
                            "FREE COUNT"
                        } else {
                            "LAP $lapNumber · NEXT: $nextMilestone"
                        },
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Reset Button
                IconButton(
                    onClick = {
                        if (count > 0) {
                            showResetDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset Counter",
                        tint = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // =================================================================
            // CENTER HERO: Dhikr Phase, Massive Tabular Count & Progress Ring
            // =================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(tapScale.value)
            ) {
                // Phase Pill (SubhanAllah / Alhamdulillah / Allahu Akbar / Seal)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = currentPhase.targetRangeLabel.uppercase(),
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            letterSpacing = 1.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arabic Typography Calligraphy
                Text(
                    text = currentPhase.arabic,
                    fontSize = 32.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Transliteration & English Meaning
                Text(
                    text = currentPhase.transliteration,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = currentPhase.translation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Massive Circular Progress Display & Number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(230.dp)
                ) {
                    // Ambient glow background
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Progress ring with subtle background track
                    CircularProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.size(210.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        strokeWidth = 4.dp,
                        strokeCap = StrokeCap.Round
                    )

                    // Massive Tabular Digits
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = count.toString(),
                            style = TabularClockStyle.copy(
                                fontSize = if (count >= 1000) 64.sp else 86.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-1).sp
                            )
                        )

                        Text(
                            text = when (targetMode) {
                                TasbihTargetMode.FATIMA_100 -> "OF 100"
                                TasbihTargetMode.TARGET_33 -> "OF 33"
                                TasbihTargetMode.TARGET_100 -> "OF 100"
                                TasbihTargetMode.FREE -> "TOTAL COUNT"
                            },
                            style = TabularClockStyle.copy(
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // =================================================================
            // BOTTOM SECTION: Tactile Tap Prompt & Target Mode Selector
            // =================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "TAP ANYWHERE ON SCREEN TO COUNT",
                    style = TabularClockStyle.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = promptAlpha)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Target Mode Selection Bar (33 / 100 / Free)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TasbihTargetMode.entries.forEach { mode ->
                        val isSelected = mode == targetMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onSelectMode(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (mode) {
                                    TasbihTargetMode.FATIMA_100 -> "Fatima (100)"
                                    TasbihTargetMode.TARGET_33 -> "33"
                                    TasbihTargetMode.TARGET_100 -> "100"
                                    TasbihTargetMode.FREE -> "Free"
                                },
                                style = TabularClockStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // Reset Confirmation Dialog to protect user from accidental clears
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "Reset Tasbih Counter?",
                        style = TabularClockStyle.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                text = {
                    Text(
                        text = "Current count of $count will be reset back to 0.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            onReset()
                            triggerClockTickHaptic(view, context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

/**
 * Triggers instant system [HapticFeedbackConstants.CLOCK_TICK] on every tap.
 * Falls back to minimal predefined tick on platforms where view haptics are suppressed.
 */
private fun triggerClockTickHaptic(view: View, context: Context) {
    val performed = view.performHapticFeedback(
        HapticFeedbackConstants.CLOCK_TICK,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
    if (!performed) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10L)
        }
    }
}

/**
 * Triggers a distinct tactile double-pulse waveform vibration at milestone intervals
 * (33, 66, 99, 100) so the user senses phase completion without looking at the screen.
 */
private fun triggerMilestoneVibration(context: Context) {
    val vibrator = getVibrator(context) ?: return
    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 50, 40, 110)
        val amplitudes = intArrayOf(0, 220, 0, 255)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 50, 40, 110), -1)
    }
}

/**
 * Resolves the system Vibrator instance compatible with Android 8.0 through 15 (SDK 26 - 36).
 */
private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

@Preview(name = "Tasbih Screen OLED Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TasbihScreenPreview() {
    HayyaTheme {
        TasbihScreen(
            count = 33,
            targetMode = TasbihTargetMode.FATIMA_100,
            onIncrement = {},
            onReset = {},
            onSelectMode = {},
            onBackClick = {}
        )
    }
}
