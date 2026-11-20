package dev.hayya.core.ui.screens

import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.qibla.QiblaCalculator
import dev.hayya.core.system.sensor.QiblaOrientationSensor
import dev.hayya.core.ui.components.SectionPageHeader
import dev.hayya.core.ui.theme.HayyaSuccess
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Clean, luminous Qibla Finder screen with intuitive turn guidance and tactile alignment feedback.
 */
@Composable
fun QiblaScreen(
    locationName: String,
    latitude: Double,
    longitude: Double,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    val view = LocalView.current

    var filteredAzimuth by remember { mutableFloatStateOf(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var hasSensors by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sensor = QiblaOrientationSensor(context, smoothingAlpha = 0.22f)
        hasSensors = sensor.hasRequiredSensors()
        sensor.start { newAzimuth, accuracy ->
            filteredAzimuth = newAzimuth
            sensorAccuracy = accuracy
        }
        onDispose {
            sensor.stop()
        }
    }

    val qiblaBearing = remember(latitude, longitude) {
        QiblaCalculator.calculateBearing(latitude, longitude)
    }
    val distanceKm = remember(latitude, longitude) {
        QiblaCalculator.calculateDistanceKm(latitude, longitude)
    }
    val deviation = remember(filteredAzimuth, qiblaBearing) {
        QiblaCalculator.calculateDeviation(filteredAzimuth.toDouble(), qiblaBearing)
    }
    val isAligned = remember(deviation) {
        QiblaCalculator.isAligned(filteredAzimuth.toDouble(), qiblaBearing, toleranceDegrees = 2.0)
    }

    var wasAligned by remember { mutableStateOf(false) }
    var lastHapticTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isAligned) {
        val now = System.currentTimeMillis()
        if (isAligned && !wasAligned && (now - lastHapticTimestamp > 800L)) {
            lastHapticTimestamp = now
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                @Suppress("DEPRECATION")
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        wasAligned = isAligned
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aligned_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val activeAccent by animateColorAsState(
        targetValue = if (isAligned) HayyaSuccess else MaterialTheme.colorScheme.primary,
        label = "qibla_accent"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header & Friendly Guidance Pill
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SectionPageHeader(
                    title = "Qibla Finder",
                    subtitle = locationName.substringBefore(","),
                    onBackClick = onBackClick
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Clear, unmistakable guidance pill
                val turnDeg = abs(deviation).roundToInt()
                val guidanceText = when {
                    !hasSensors -> "Compass sensor unavailable on this device"
                    isAligned -> "You are facing the Qibla"
                    deviation > 0 -> "Turn right $turnDeg°"
                    else -> "Turn left $turnDeg°"
                }

                Surface(
                    shape = CircleShape,
                    color = activeAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAligned) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = activeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = guidanceText,
                            style = TabularClockStyle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent
                            )
                        )
                    }
                }
            }

            // Center Compass Visual
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .size(286.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        activeAccent.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // Rotating Compass Dial
                Canvas(
                    modifier = Modifier
                        .size(274.dp)
                        .graphicsLayer {
                            rotationZ = -filteredAzimuth
                        }
                ) {
                    val centerOffset = this.center
                    val radius = size.minDimension / 2f
                    val stroke1px = 1.dp.toPx()
                    val majorTickLength = 12.dp.toPx()
                    val minorTickLength = 6.dp.toPx()

                    // Outer dial ring
                    drawCircle(
                        color = if (isAligned) activeAccent else outlineColor,
                        radius = radius - stroke1px,
                        style = Stroke(width = if (isAligned) 2.dp.toPx() else 1.2.dp.toPx())
                    )

                    // Inner subtle guide ring
                    drawCircle(
                        color = outlineColor.copy(alpha = 0.25f),
                        radius = radius * 0.68f,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Degree ticks
                    for (deg in 0 until 360 step 5) {
                        val isMajor = deg % 30 == 0
                        val isCardinal = deg % 90 == 0
                        val tickLength = if (isMajor) majorTickLength else minorTickLength
                        val tickColor = when {
                            deg == 0 -> primaryColor
                            isCardinal -> onSurfaceColor
                            isMajor -> onSurfaceVariantColor.copy(alpha = 0.7f)
                            else -> outlineColor.copy(alpha = 0.45f)
                        }
                        val angleRad = Math.toRadians((deg - 90).toDouble())
                        val cosVal = cos(angleRad).toFloat()
                        val sinVal = sin(angleRad).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(
                                centerOffset.x + (radius - stroke1px - tickLength) * cosVal,
                                centerOffset.y + (radius - stroke1px - tickLength) * sinVal
                            ),
                            end = Offset(
                                centerOffset.x + (radius - stroke1px) * cosVal,
                                centerOffset.y + (radius - stroke1px) * sinVal
                            ),
                            strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Pointer Line from center toward Kaaba bearing
                    val qiblaRad = Math.toRadians(qiblaBearing - 90.0)
                    val markerRadius = radius - stroke1px - majorTickLength - 8.dp.toPx()
                    val markerEnd = Offset(
                        centerOffset.x + markerRadius * cos(qiblaRad).toFloat(),
                        centerOffset.y + markerRadius * sin(qiblaRad).toFloat()
                    )

                    drawLine(
                        color = activeAccent.copy(alpha = 0.55f),
                        start = centerOffset,
                        end = markerEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = activeAccent,
                        radius = 7.dp.toPx(),
                        center = markerEnd
                    )
                }

                // Rotating N/E/S/W & Kaaba Marker
                Box(
                    modifier = Modifier
                        .size(274.dp)
                        .graphicsLayer { rotationZ = -filteredAzimuth }
                ) {
                    Text(
                        text = "N",
                        style = TabularClockStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 18.dp)
                    )
                    Text(
                        text = "E",
                        style = TabularClockStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 18.dp)
                    )
                    Text(
                        text = "S",
                        style = TabularClockStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    )
                    Text(
                        text = "W",
                        style = TabularClockStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 18.dp)
                    )

                    // Kaaba Label on Dial
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = qiblaBearing.toFloat() }
                    ) {
                        Text(
                            text = "KAABA",
                            style = TabularClockStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = activeAccent
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 42.dp)
                        )
                    }
                }

                // Fixed Top Forward Pointer
                Icon(
                    imageVector = Icons.Filled.ArrowDropUp,
                    contentDescription = "Phone forward direction",
                    tint = activeAccent,
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer { translationY = (-10).dp.toPx() }
                )

                // Center Minimalist Kaaba Cube
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(
                            width = if (isAligned) 2.dp else 1.2.dp,
                            color = if (isAligned) activeAccent else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(activeAccent)
                    )
                }
            }

            // Bottom Simple Facts Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bearingFormatted = QiblaCalculator.formatBearingReadout(qiblaBearing)
                Text(
                    text = bearingFormatted,
                    style = TabularClockStyle.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAligned) activeAccent else MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Qibla Angle",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f°", qiblaBearing),
                                style = TabularClockStyle.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "To Makkah",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%,d km", distanceKm.roundToInt()),
                                style = TabularClockStyle.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                if (hasSensors && (sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Move your phone in a figure-8 to improve accuracy",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Qibla Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QiblaScreenPreview() {
    HayyaTheme {
        QiblaScreen(
            locationName = "London, United Kingdom",
            latitude = 51.5074,
            longitude = -0.1278,
            onBackClick = {}
        )
    }
}
