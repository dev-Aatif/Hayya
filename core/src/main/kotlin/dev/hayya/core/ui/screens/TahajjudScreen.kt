package dev.hayya.core.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hayya.core.ui.components.SectionPageHeader
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.IshaTint
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TabularCountdownStyle
import dev.hayya.core.ui.viewmodel.PrayerCountdownUiState
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dedicated visual Night Prayer (Tahajjud) & Fasting Windows page.
 */
@Composable
fun TahajjudScreen(
    state: PrayerDashboardUiState,
    onBackClick: () -> Unit,
    onToggleTahajjudAlarm: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    countdownStateFlow: StateFlow<PrayerCountdownUiState>? = null
) {
    BackHandler(onBack = onBackClick)
    val liveTick = countdownStateFlow?.collectAsStateWithLifecycle()?.value
    val liveTahajjudActive = liveTick?.isTahajjudActiveNow ?: state.isTahajjudActiveNow
    val liveTahajjudCountdown = liveTick?.tahajjudCountdown ?: state.tahajjudCountdown
    val liveFastingCountdown = liveTick?.fastingCountdown ?: state.fastingCountdown
    val liveIsFastingToIftar = liveTick?.isFastingCountdownToIftar ?: state.isFastingCountdownToIftar

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionPageHeader(
                title = "Night & Fasting",
                subtitle = "Tahajjud, Suhoor & Iftar",
                onBackClick = onBackClick,
                badgeText = if (liveTahajjudActive) "Active Now" else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Night Sky Arc Visual Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NightSkyArcVisual(
                        isTahajjudActive = liveTahajjudActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (liveTahajjudActive) {
                            "Last Third of the Night"
                        } else {
                            "Last Third Begins In"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = liveTahajjudCountdown,
                        style = TabularCountdownStyle.copy(
                            fontSize = 40.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (liveTahajjudActive) {
                            "Best time for Tahajjud until Fajr at ${state.suhoorCutoffFormatted}"
                        } else {
                            "Tonight at ${state.tahajjudStartTimeFormatted}"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Night Milestones Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NightTimeTile(
                    title = "Sunset",
                    subtitle = "Night begins",
                    time = state.iftarFormatted,
                    isHighlighted = false,
                    modifier = Modifier.weight(1f)
                )
                NightTimeTile(
                    title = "Midnight",
                    subtitle = "Halfway point",
                    time = state.islamicMidnightFormatted,
                    isHighlighted = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NightTimeTile(
                    title = "Last Third",
                    subtitle = "Best for Tahajjud",
                    time = state.tahajjudStartTimeFormatted,
                    isHighlighted = true,
                    modifier = Modifier.weight(1f)
                )
                NightTimeTile(
                    title = "Dawn (Fajr)",
                    subtitle = "Suhoor ends",
                    time = state.suhoorCutoffFormatted,
                    isHighlighted = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Friendly Tahajjud Wake-Up Alarm Switch Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    if (state.userPreferences.tahajjudAlarmEnabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tahajjud Wake Alarm",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gentle alert at ${state.tahajjudStartTimeFormatted}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = state.userPreferences.tahajjudAlarmEnabled,
                        onCheckedChange = onToggleTahajjudAlarm,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fasting & Suhoor / Iftar Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fasting Times",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        val fastLabel = if (liveIsFastingToIftar) {
                            "Iftar in $liveFastingCountdown"
                        } else {
                            "Suhoor in $liveFastingCountdown"
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = fastLabel,
                                style = TabularClockStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (state.isSunnahFastingToday || state.isSunnahFastingTomorrow || state.islamicDateInfo.isRamadan) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val tip = when {
                            state.isSunnahFastingToday || state.islamicDateInfo.isRamadan ->
                                state.fastingDayTitle.ifEmpty { "Recommended fasting day today" }
                            else -> "Tomorrow: ${state.tomorrowFastingDayTitle}"
                        }
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Suhoor Ends",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.suhoorCutoffFormatted,
                                style = TabularClockStyle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Iftar Time",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.iftarFormatted,
                                style = TabularClockStyle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    if (state.sehriIhtiyatFormatted.isNotEmpty() || state.ishraqTimeFormatted.isNotEmpty() || state.zawalWindowFormatted.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.sehriIhtiyatFormatted.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Sehri Stop",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.sehriIhtiyatFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                            if (state.ishraqTimeFormatted.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Ishraq",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.ishraqTimeFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                            if (state.zawalWindowFormatted.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Zawal",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.zawalWindowFormatted,
                                        style = TabularClockStyle.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                        if (state.mosqueSeasonLabel.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mosque Schedule: ${state.mosqueSeasonLabel} • 10m Sehri safety",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Readable Inspiration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Why the Last Third?",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "“Our Lord descends every night to the lowest heaven when the last third of the night remains, saying: ‘Who calls upon Me that I may answer him? Who asks of Me that I may give him?’”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sahih al-Bukhari 1145 • Sahih Muslim 758",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun NightSkyArcVisual(
    isTahajjudActive: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val nightBlue = IshaTint

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padH = 26.dp.toPx()
        val diameter = (w - padH * 2f).coerceAtMost((h - 14.dp.toPx()) * 2f)
        val left = (w - diameter) / 2f
        val top = h - (diameter / 2f) - 10.dp.toPx()
        val center = Offset(left + diameter / 2f, top + diameter / 2f)
        val radius = diameter / 2f

        // Subtle star dots in the upper sky
        val stars = listOf(
            Offset(w * 0.22f, h * 0.25f) to 1.8.dp.toPx(),
            Offset(w * 0.35f, h * 0.14f) to 1.4.dp.toPx(),
            Offset(w * 0.68f, h * 0.16f) to 2.dp.toPx(),
            Offset(w * 0.80f, h * 0.30f) to 1.5.dp.toPx()
        )
        stars.forEach { (pos, r) ->
            drawCircle(color = nightBlue.copy(alpha = 0.5f), radius = r, center = pos)
        }

        // First two-thirds of the night (Sunset -> Last Third)
        drawArc(
            color = track,
            startAngle = 180f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(diameter, diameter),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Highlighted Last Third (300° to 360°)
        drawArc(
            brush = Brush.horizontalGradient(
                colors = listOf(primary.copy(alpha = 0.7f), primary)
            ),
            startAngle = 300f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(diameter, diameter),
            style = Stroke(width = if (isTahajjudActive) 7.dp.toPx() else 5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Marker dot at the start of the Last Third (angle 300°)
        val startRad = Math.toRadians(300.0)
        val markerPos = Offset(
            center.x + radius * cos(startRad).toFloat(),
            center.y + radius * sin(startRad).toFloat()
        )
        drawCircle(
            color = primary.copy(alpha = 0.35f),
            radius = 12.dp.toPx(),
            center = markerPos
        )
        drawCircle(
            color = primary,
            radius = 5.dp.toPx(),
            center = markerPos
        )

        // Horizon baseline
        drawLine(
            color = track.copy(alpha = 0.6f),
            start = Offset(left - 10.dp.toPx(), center.y),
            end = Offset(left + diameter + 10.dp.toPx(), center.y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun NightTimeTile(
    title: String,
    subtitle: String,
    time: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = TabularClockStyle.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(name = "Tahajjud Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TahajjudScreenPreview() {
    HayyaTheme {
        TahajjudScreen(
            state = PrayerDashboardUiState(
                tahajjudCountdown = "04:12:30",
                tahajjudStartTimeFormatted = "03:45 AM",
                islamicMidnightFormatted = "11:45 PM",
                suhoorCutoffFormatted = "05:15 AM",
                iftarFormatted = "06:20 PM"
            ),
            onBackClick = {},
            onToggleTahajjudAlarm = {}
        )
    }
}
