package dev.hayya.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.CityCatalog
import dev.hayya.core.domain.model.CityPreset
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.prayerAccentColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Human-friendly title-cased prayer name (e.g., "Fajr", "Jumu'ah").
 */
fun friendlyPrayerName(prayerType: PrayerType, isFriday: Boolean = false): String = when (prayerType) {
    PrayerType.FAJR -> "Fajr"
    PrayerType.SUNRISE -> "Sunrise"
    PrayerType.DHUHR -> if (isFriday) "Jumu'ah" else "Dhuhr"
    PrayerType.ASR -> "Asr"
    PrayerType.MAGHRIB -> "Maghrib"
    PrayerType.ISHA -> "Isha"
}

/**
 * Short plain-English time-of-day cue so anyone (including kids) instantly recognizes each prayer.
 */
fun prayerTimeOfDayHint(prayerType: PrayerType): String = when (prayerType) {
    PrayerType.FAJR -> "Dawn"
    PrayerType.SUNRISE -> "Sun Up"
    PrayerType.DHUHR -> "Midday"
    PrayerType.ASR -> "Afternoon"
    PrayerType.MAGHRIB -> "Sunset"
    PrayerType.ISHA -> "Night"
}

/**
 * Bespoke Canvas celestial artwork for each prayer time of day.
 */
@Composable
fun PrayerCelestialIcon(
    prayerType: PrayerType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tintOverride: Color? = null
) {
    val accent = tintOverride ?: prayerAccentColor(prayerType)
    val horizonColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = (w * 0.08f).coerceAtLeast(1.5f)

        when (prayerType) {
            PrayerType.FAJR -> {
                // Dawn: soft horizon line + glowing crescent/orb peeking gently
                val hy = h * 0.70f
                drawCircle(
                    color = accent.copy(alpha = 0.25f),
                    radius = w * 0.34f,
                    center = Offset(cx, hy - h * 0.08f)
                )
                drawArc(
                    color = accent,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.22f, hy - w * 0.22f),
                    size = Size(w * 0.44f, w * 0.44f)
                )
                drawLine(
                    color = horizonColor,
                    start = Offset(w * 0.14f, hy),
                    end = Offset(w * 0.86f, hy),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                // Small morning star
                drawCircle(
                    color = accent,
                    radius = w * 0.055f,
                    center = Offset(w * 0.76f, h * 0.26f)
                )
            }

            PrayerType.SUNRISE -> {
                // Rising sun with radiating rays above horizon
                val hy = h * 0.72f
                drawArc(
                    color = accent,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.24f, hy - w * 0.24f),
                    size = Size(w * 0.48f, w * 0.48f)
                )
                for (angleDeg in listOf(-145, -115, -90, -65, -35)) {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val r1 = w * 0.31f
                    val r2 = w * 0.42f
                    drawLine(
                        color = accent,
                        start = Offset(cx + r1 * cos(rad).toFloat(), hy + r1 * sin(rad).toFloat()),
                        end = Offset(cx + r2 * cos(rad).toFloat(), hy + r2 * sin(rad).toFloat()),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = horizonColor,
                    start = Offset(w * 0.12f, hy),
                    end = Offset(w * 0.88f, hy),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            PrayerType.DHUHR -> {
                // Midday radiant sun
                drawCircle(
                    color = accent.copy(alpha = 0.22f),
                    radius = w * 0.34f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.21f,
                    center = Offset(cx, cy)
                )
                for (i in 0 until 8) {
                    val rad = Math.toRadians((i * 45).toDouble())
                    val r1 = w * 0.30f
                    val r2 = w * 0.42f
                    drawLine(
                        color = accent,
                        start = Offset(cx + r1 * cos(rad).toFloat(), cy + r1 * sin(rad).toFloat()),
                        end = Offset(cx + r2 * cos(rad).toFloat(), cy + r2 * sin(rad).toFloat()),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            PrayerType.ASR -> {
                // Afternoon lower sun with soft glow
                val sunCenter = Offset(w * 0.54f, h * 0.44f)
                drawCircle(
                    color = accent.copy(alpha = 0.20f),
                    radius = w * 0.33f,
                    center = sunCenter
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.20f,
                    center = sunCenter
                )
                // Gentle slant rays
                for (angleDeg in listOf(-135, -90, -45, 0, 45)) {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val r1 = w * 0.27f
                    val r2 = w * 0.36f
                    drawLine(
                        color = accent.copy(alpha = 0.8f),
                        start = Offset(sunCenter.x + r1 * cos(rad).toFloat(), sunCenter.y + r1 * sin(rad).toFloat()),
                        end = Offset(sunCenter.x + r2 * cos(rad).toFloat(), sunCenter.y + r2 * sin(rad).toFloat()),
                        strokeWidth = strokeW * 0.9f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = horizonColor,
                    start = Offset(w * 0.14f, h * 0.80f),
                    end = Offset(w * 0.86f, h * 0.80f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            PrayerType.MAGHRIB -> {
                // Sunset: warm sun dipping behind horizon
                val hy = h * 0.66f
                drawCircle(
                    color = accent.copy(alpha = 0.24f),
                    radius = w * 0.32f,
                    center = Offset(cx, hy - h * 0.04f)
                )
                drawArc(
                    color = accent,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - w * 0.23f, hy - w * 0.23f),
                    size = Size(w * 0.46f, w * 0.46f)
                )
                drawLine(
                    color = horizonColor,
                    start = Offset(w * 0.12f, hy),
                    end = Offset(w * 0.88f, hy),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                // Water/horizon reflection line
                drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = Offset(w * 0.28f, hy + h * 0.13f),
                    end = Offset(w * 0.72f, hy + h * 0.13f),
                    strokeWidth = strokeW * 0.85f,
                    cap = StrokeCap.Round
                )
            }

            PrayerType.ISHA -> {
                // Night: crescent moon + stars
                drawCircle(
                    color = accent,
                    radius = w * 0.26f,
                    center = Offset(cx - w * 0.04f, cy + h * 0.03f)
                )
                drawCircle(
                    color = surfaceColor,
                    radius = w * 0.22f,
                    center = Offset(cx + w * 0.08f, cy - h * 0.05f)
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.055f,
                    center = Offset(w * 0.77f, h * 0.26f)
                )
                drawCircle(
                    color = accent.copy(alpha = 0.65f),
                    radius = w * 0.04f,
                    center = Offset(w * 0.24f, h * 0.24f)
                )
            }
        }
    }
}

/**
 * Luminous half-circle sky arc visualizing progress through the current prayer window.
 */
@Composable
fun SolarSkyArc(
    progress: Float,
    nextPrayerType: PrayerType,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "solar_arc_progress"
    )
    val accent = prayerAccentColor(nextPrayerType)
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
    val horizonColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val paddingH = 18.dp.toPx()
        val arcDiameter = (w - paddingH * 2f).coerceAtMost((h - 16.dp.toPx()) * 2f)
        val left = (w - arcDiameter) / 2f
        val top = h - (arcDiameter / 2f) - 10.dp.toPx()
        val arcSize = Size(arcDiameter, arcDiameter)
        val center = Offset(left + arcDiameter / 2f, top + arcDiameter / 2f)
        val radius = arcDiameter / 2f

        // Subtle horizon baseline
        drawLine(
            color = horizonColor,
            start = Offset(left - 8.dp.toPx(), center.y),
            end = Offset(left + arcDiameter + 8.dp.toPx(), center.y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Background track arc
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = arcSize,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Elapsed glowing progress arc
        val sweep = 180f * animatedProgress
        if (sweep > 0.5f) {
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(primary.copy(alpha = 0.45f), accent, primary)
                ),
                startAngle = 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(left, top),
                size = arcSize,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Horizon anchor dots
        drawCircle(
            color = primary.copy(alpha = 0.55f),
            radius = 3.dp.toPx(),
            center = Offset(left, center.y)
        )
        drawCircle(
            color = trackColor,
            radius = 3.dp.toPx(),
            center = Offset(left + arcDiameter, center.y)
        )

        // Gliding celestial orb along the arc
        val angleRad = Math.toRadians((180.0 + sweep))
        val orbX = center.x + radius * cos(angleRad).toFloat()
        val orbY = center.y + radius * sin(angleRad).toFloat()
        val orbPos = Offset(orbX, orbY)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                center = orbPos,
                radius = 16.dp.toPx()
            ),
            radius = 16.dp.toPx(),
            center = orbPos
        )
        drawCircle(
            color = accent,
            radius = 5.5.dp.toPx(),
            center = orbPos
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 2.2.dp.toPx(),
            center = orbPos
        )
    }
}

/**
 * Readable, friendly alert mode badge with icon + short clear label ("Adhan", "Banner", "Muted").
 */
@Composable
fun AlertModeBadge(
    mode: AlertMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label, containerColor, contentColor) = when (mode) {
        AlertMode.FULL_ALARM -> Quad(
            Icons.Outlined.NotificationsActive,
            "Adhan",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.primary
        )
        AlertMode.GENTLE_BANNER -> Quad(
            Icons.Outlined.Notifications,
            "Silent",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        AlertMode.MUTED -> Quad(
            Icons.Outlined.NotificationsOff,
            "Off",
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (mode == AlertMode.FULL_ALARM) contentColor.copy(alpha = 0.30f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.displayName,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = TabularClockStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Consistent, generous sub-page header with 42dp back button and readable title.
 */
@Composable
fun SectionPageHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!badgeText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Text(
                    text = badgeText,
                    style = TabularClockStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Offline City Picker Bottom Sheet with search, popular cities, and optional custom coordinates.
 * Makes picking any global city effortless for both a 10-year-old and a 40-year-old.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CityPickerBottomSheet(
    currentLocationName: String,
    currentLat: Double,
    currentLng: Double,
    onSelectCity: (CityPreset) -> Unit,
    onSelectCustomCoordinates: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All Towns") }
    var showCustomCoords by remember { mutableStateOf(false) }
    var cityInput by remember(currentLocationName) { mutableStateOf(currentLocationName) }
    var latInput by remember(currentLat) { mutableStateOf(currentLat.toString()) }
    var lngInput by remember(currentLng) { mutableStateOf(currentLng.toString()) }
    var coordError by remember { mutableStateOf<String?>(null) }

    val filterOptions = listOf(
        "All Towns",
        "Larkana",
        "Qambar Shahdadkot",
        "Shikarpur",
        "Jacobabad",
        "Kashmore"
    )

    val filteredCities = remember(searchQuery, selectedFilter) {
        val base = when (selectedFilter) {
            "All Towns" -> CityCatalog.larkanaDivisionCities
            else -> CityCatalog.larkanaDivisionCities.filter { it.district == selectedFilter }
        }
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) base
        else base.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.district.contains(trimmed, ignoreCase = true) ||
                it.authorityType.contains(trimmed, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Choose Your Town",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Larkana Division • Works 100% offline",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                TextButton(onClick = { showCustomCoords = !showCustomCoords }) {
                    Text(
                        text = if (showCustomCoords) "City List" else "Coordinates",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = !showCustomCoords) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Search Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search town, taluka, or district...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Region Filter Pills (quick access to Pakistan & provinces)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterOptions) { filter ->
                            val isSelected = filter == selectedFilter
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedFilter = filter }
                            ) {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (searchQuery.isBlank() && selectedFilter == "All Towns") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Major Towns in Larkana Division",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CityCatalog.popularCities.forEach { city ->
                                val isCurrent = currentLocationName.contains(city.name, ignoreCase = true)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            onSelectCity(city)
                                            onDismiss()
                                        }
                                ) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 330.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredCities.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No matching city found",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap 'Coordinates' above to enter custom latitude and longitude.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        } else {
                            items(filteredCities, key = { "${it.name}-${it.district}-${it.country}" }) { city ->
                                val isCurrent = currentLocationName.equals(city.displayTitle, ignoreCase = true) ||
                                    currentLocationName.equals(city.name, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelectCity(city)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = city.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Text(
                                                    text = city.subtitleLabel,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showCustomCoords) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("City / Place Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it; coordError = null },
                            label = { Text("Latitude") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = { lngInput = it; coordError = null },
                            label = { Text("Longitude") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (coordError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = coordError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomCoords = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Back to Cities")
                        }
                        Button(
                            onClick = {
                                val lat = latInput.trim().toDoubleOrNull()
                                val lng = lngInput.trim().toDoubleOrNull()
                                if (lat == null || lat !in -90.0..90.0 || lng == null || lng !in -180.0..180.0) {
                                    coordError = "Latitude must be -90..90 and Longitude -180..180."
                                } else {
                                    onSelectCustomCoordinates(
                                        lat,
                                        lng,
                                        cityInput.trim().ifEmpty { "Custom Location" }
                                    )
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Save Location", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
