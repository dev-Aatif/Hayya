package dev.hayya.core.ui.screens

import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.theme.TabularClockStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Ultra-minimalist Pure OLED Qibla Compass interface.
 *
 * Visual & Behavioral Design:
 * 1. Pitch black OLED background (#000000).
 * 2. 1px geometric gold ring with subtle degree markers and cardinal points.
 * 3. Continuous circular low-pass filtered rotation for smooth, jitter-free orientation.
 * 4. Precise tabular degree read-out (e.g. "118° ESE").
 * 5. Distinct haptic pulse (HapticFeedbackConstants.CONFIRM) triggered when aligned within ±2°.
 * 6. Minimalist geometric Kaaba representation at dead center with golden Kiswah accent.
 */
@Composable
fun QiblaScreen(
    locationName: String,
    latitude: Double,
    longitude: Double,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    var filteredAzimuth by remember { mutableFloatStateOf(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var hasSensors by remember { mutableStateOf(true) }

    // Start/Stop sensor lifecycle safely bound to Composition
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

    // Trigger haptic pulse (CONFIRM) only once upon crossing into the ±2° alignment zone
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

    // Subtle breathing glow animation when aligned
    val infiniteTransition = rememberInfiniteTransition(label = "aligned_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =================================================================
            // TOP BAR: Navigation, Section Title & Location Coordinates
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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

                    // Section Title
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "QIBLA COMPASS",
                            style = TabularClockStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Explore,
                            contentDescription = null,
                            tint = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Location Subtitle
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = locationName.uppercase(),
                        style = TabularClockStyle.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    val latFormatted = String.format("%.4f° %s", abs(latitude), if (latitude >= 0) "N" else "S")
                    val lngFormatted = String.format("%.4f° %s", abs(longitude), if (longitude >= 0) "E" else "W")

                    Text(
                        text = "$latFormatted · $lngFormatted",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // =================================================================
            // CENTER: 1px Geometric Gold Ring & Rotating Compass Dial
            // =================================================================
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(290.dp)
                    .padding(vertical = 12.dp)
            ) {
                // Ambient Radial Glow when aligned within ±2°
                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // 1px Geometric Gold Ring with Compass Ticks
                Canvas(
                    modifier = Modifier
                        .size(270.dp)
                        .graphicsLayer {
                            // Smooth filtered rotation counter-clockwise with device heading
                            rotationZ = -filteredAzimuth
                        }
                ) {
                    val centerOffset = this.center
                    val radius = size.minDimension / 2f
                    val stroke1px = 1.dp.toPx()
                    val majorTickLength = 10.dp.toPx()
                    val minorTickLength = 5.dp.toPx()

                    // Outer 1px geometric boundary ring
                    drawCircle(
                        color = if (isAligned) primaryColor else outlineColor,
                        radius = radius - stroke1px,
                        style = Stroke(width = if (isAligned) 1.5.dp.toPx() else 1.dp.toPx())
                    )

                    // Degree tick marks (every 5° minor, every 30° major)
                    for (deg in 0 until 360 step 5) {
                        val isMajor = deg % 30 == 0
                        val isCardinal = deg % 90 == 0
                        val tickLength = if (isMajor) majorTickLength else minorTickLength

                        val tickColor = when {
                            deg == 0 -> primaryColor // North is accent gold
                            isCardinal -> onSurfaceColor
                            isMajor -> onSurfaceVariantColor.copy(alpha = 0.7f)
                            else -> outlineColor.copy(alpha = 0.45f)
                        }

                        val angleRad = Math.toRadians((deg - 90).toDouble())
                        val cosVal = cos(angleRad).toFloat()
                        val sinVal = sin(angleRad).toFloat()

                        val startX = centerOffset.x + (radius - stroke1px - tickLength) * cosVal
                        val startY = centerOffset.y + (radius - stroke1px - tickLength) * sinVal
                        val endX = centerOffset.x + (radius - stroke1px) * cosVal
                        val endY = centerOffset.y + (radius - stroke1px) * sinVal

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isCardinal) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Kaaba Destination Pin plotted at exact great-circle bearing on the dial
                    val qiblaRad = Math.toRadians((qiblaBearing - 90.0))
                    val markerRadius = radius - stroke1px - majorTickLength - 8.dp.toPx()
                    val markerX = centerOffset.x + markerRadius * cos(qiblaRad).toFloat()
                    val markerY = centerOffset.y + markerRadius * sin(qiblaRad).toFloat()

                    drawCircle(
                        color = primaryColor,
                        radius = 5.dp.toPx(),
                        center = Offset(markerX, markerY)
                    )
                }

                // Rotating Cardinal Labels (N, E, S, W)
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .graphicsLayer {
                            rotationZ = -filteredAzimuth
                        }
                ) {
                    // North (0°)
                    Text(
                        text = "N",
                        style = TabularClockStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 15.dp)
                    )

                    // East (90°)
                    Text(
                        text = "E",
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 15.dp)
                    )

                    // South (180°)
                    Text(
                        text = "S",
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 15.dp)
                    )

                    // West (270°)
                    Text(
                        text = "W",
                        style = TabularClockStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 15.dp)
                    )

                    // Kaaba Target Flag marker on the rotating dial
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = qiblaBearing.toFloat()
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 34.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "KAABA",
                                style = TabularClockStyle.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Fixed Top Forward Arrow (indicates phone's forward pointing axis at 12 o'clock)
                Icon(
                    imageVector = Icons.Filled.ArrowDropUp,
                    contentDescription = "Forward Direction",
                    tint = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer { translationY = (-14).dp.toPx() }
                )

                // Dead Center Minimalist Geometric Kaaba Icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                width = if (isAligned) 1.5.dp else 1.dp,
                                color = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Golden Kiswah belt band across upper third of the Kaaba cube
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .padding(top = 10.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Alignment Pill / Turn Guidance
                    if (isAligned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ALIGNED",
                                    style = TabularClockStyle.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    } else {
                        val turnDeg = abs(deviation).roundToInt()
                        val turnDir = if (deviation > 0) "TURN RIGHT" else "TURN LEFT"
                        Text(
                            text = "$turnDir ${turnDeg}°",
                            style = TabularClockStyle.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        )
                    }
                }
            }

            // =================================================================
            // BOTTOM SECTION: Large Degree Read-out & Compass Metadata
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Precise Degree Read-out (e.g. "118° ESE")
                val bearingFormatted = QiblaCalculator.formatBearingReadout(qiblaBearing)
                Text(
                    text = bearingFormatted,
                    style = TabularClockStyle.copy(
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        color = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isAligned) {
                        "Accurately aligned within ±2° of Kaaba"
                    } else {
                        "Spherical great-circle azimuth to Kaaba"
                    },
                    fontSize = 12.sp,
                    color = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Card with Great-Circle Geodetic Telemetry
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BEARING",
                                style = TabularClockStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format("%.1f°", qiblaBearing),
                                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "HEADING",
                                style = TabularClockStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format("%.1f°", filteredAzimuth),
                                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DISTANCE",
                                style = TabularClockStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val distFormatted = String.format("%,d km", distanceKm.roundToInt())
                            Text(
                                text = distFormatted,
                                style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }

                // Sensor calibration notice if accuracy is low or hardware sensors absent
                if (!hasSensors) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Device magnetometer unavailable. Calculated Qibla bearing is exact, but real-time orientation tracking is disabled.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                } else if (sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tilt device in a figure-8 motion to calibrate compass sensor",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Qibla Screen OLED Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
