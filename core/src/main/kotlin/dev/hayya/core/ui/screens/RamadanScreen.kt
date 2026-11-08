package dev.hayya.core.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.ramadan.DailyFastRecord
import dev.hayya.core.domain.ramadan.EidGuide
import dev.hayya.core.domain.ramadan.FastStatus
import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.domain.ramadan.IslamicDateInfo
import dev.hayya.core.domain.ramadan.RamadanDaySchedule
import dev.hayya.core.domain.ramadan.RamadanDua
import dev.hayya.core.domain.ramadan.RamadanDuaCategory
import dev.hayya.core.domain.ramadan.RamadanDuasStore
import dev.hayya.core.domain.ramadan.RamadanMonthProgress
import dev.hayya.core.domain.ramadan.RamadanSettings
import dev.hayya.core.ui.theme.TabularClockStyle
import dev.hayya.core.ui.theme.TabularCountdownStyle
import dev.hayya.core.ui.viewmodel.PrayerDashboardUiState
import dev.hayya.core.ui.viewmodel.PrayerViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * Top-level tabs inside the Ramadan & Eid screen.
 */
enum class RamadanHubSection(val label: String) {
    TRACKER("Tracker"),
    DUAS("Duas"),
    TIMETABLE("Timetable"),
    EID("Eid & Takbirat"),
    ALERTS("Alerts")
}

/**
 * Full-screen dedicated Ramadan & Eid Hub.
 *
 * Provides:
 * - Live Suhoor cutoff and Iftar countdown tracker
 * - 30-Day Ramadan fasting log & Qadha make-up tracker
 * - Authentic Hadith Duas for Iftar, Suhoor, Laylat al-Qadr & Ashras
 * - 30-Day computed Ramadan timetable with Suhoor/Iftar timings
 * - Celebratory Eid al-Fitr & Eid al-Adha guides, Takbirat, and Sunnahs
 * - Suhoor wake-up alarm and Iftar reminder settings
 */
@Composable
fun RamadanScreen(
    state: PrayerDashboardUiState,
    calculationEngine: PrayerCalculationEngine,
    onBackClick: () -> Unit,
    onToggleTodayFast: () -> Unit,
    onSetFastStatus: (LocalDate, FastStatus) -> Unit,
    onCycleFastStatus: (LocalDate) -> Unit,
    onUpdateQadhaMadeUp: (Int) -> Unit,
    onUpdateSuhoorAlarm: (Boolean, Int?) -> Unit,
    onUpdateIftarAlert: (Boolean) -> Unit,
    onUpdateEidAlert: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(RamadanHubSection.TRACKER) }
    var selectedDuaCategory by remember { mutableStateOf(RamadanDuaCategory.ALL) }
    val view = LocalView.current

    BackHandler(onBack = onBackClick)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RAMADAN & EID",
                        style = TabularClockStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = state.islamicDateInfo.formattedHijriDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                if (state.islamicDateInfo.isRamadan) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DAY ${state.islamicDateInfo.hijriDay}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Live Suhoor & Iftar Hero Card
                SuhoorIftarHeroCard(
                    state = state,
                    onToggleTodayFast = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggleTodayFast()
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Section Navigation Pills (Clean minimal segmented controls)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RamadanHubSection.entries.toTypedArray()) { section ->
                        val isSelected = section == selectedSection
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                    CircleShape
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    selectedSection = section
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = section.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section Details
                when (selectedSection) {
                    RamadanHubSection.TRACKER -> {
                        FastingTrackerSection(
                            state = state,
                            onCycleFastStatus = onCycleFastStatus,
                            onUpdateQadhaMadeUp = onUpdateQadhaMadeUp
                        )
                    }
                    RamadanHubSection.DUAS -> {
                        DuasSection(
                            selectedCategory = selectedDuaCategory,
                            onSelectCategory = { selectedDuaCategory = it }
                        )
                    }
                    RamadanHubSection.TIMETABLE -> {
                        TimetableSection(
                            state = state,
                            calculationEngine = calculationEngine
                        )
                    }
                    RamadanHubSection.EID -> {
                        EidSection()
                    }
                    RamadanHubSection.ALERTS -> {
                        AlertsSection(
                            settings = state.ramadanSettings,
                            onUpdateSuhoorAlarm = onUpdateSuhoorAlarm,
                            onUpdateIftarAlert = onUpdateIftarAlert,
                            onUpdateEidAlert = onUpdateEidAlert
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

/**
 * Prominent Suhoor and Iftar dual card with live countdown timer and instant fast toggle.
 */
@Composable
private fun SuhoorIftarHeroCard(
    state: PrayerDashboardUiState,
    onToggleTodayFast: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Dual Times: Suhoor Cutoff & Iftar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SUHOOR CUTOFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.suhoorCutoffFormatted,
                        style = TabularClockStyle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Fajr Dawn",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "IFTAR TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.iftarFormatted,
                        style = TabularClockStyle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Maghrib Sunset",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Countdown Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.isFastingCountdownToIftar) "COUNTDOWN TO IFTAR" else "COUNTDOWN TO SUHOOR CUTOFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.fastingCountdown,
                        style = TabularCountdownStyle.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Tap "I Am Fasting Today" Button
            val isFastedToday = state.todayFastRecord?.status == FastStatus.FASTED
            Button(
                onClick = onToggleTodayFast,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFastedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFastedToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isFastedToday) Icons.Outlined.Check else Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFastedToday) "Fasting Logged For Today ✓" else "Log Today's Fast (Tap to record)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 30-Day Ramadan Fasting Tracker Grid and Qadha make-up tracker.
 */
@Composable
private fun FastingTrackerSection(
    state: PrayerDashboardUiState,
    onCycleFastStatus: (LocalDate) -> Unit,
    onUpdateQadhaMadeUp: (Int) -> Unit
) {
    val progress = state.ramadanProgress
    val view = LocalView.current

    Column {
        // Summary Metrics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAMADAN SAW LOG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${progress.fastedCount} / ${progress.totalDays} Fasted",
                        style = TabularClockStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress.completionPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Fasted", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${progress.fastedCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Missed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${progress.missedCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Exempt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${progress.exemptCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Qadha Left", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${progress.qadhaRemainingCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 30-Day Grid
        Text(
            text = "30-DAY RAMADAN SCHEDULE & LOG (TAP DAY TO CYCLE)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        val start = IslamicCalendarHelper.getRamadanStartDate(state.islamicDateInfo.hijriYear)
        val days = (0 until 30).map { start.plus(it, DateTimeUnit.DAY) }

        // Render 5 columns x 6 rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            days.chunked(5).forEachIndexed { rowIndex, chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEachIndexed { colIndex, date ->
                        val dayNum = (rowIndex * 5) + colIndex + 1
                        val status = progress.dailyRecords[dayNum] ?: FastStatus.NOT_RECORDED
                        val isToday = date == state.islamicDateInfo.gregorianDate

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (status) {
                                        FastStatus.FASTED -> MaterialTheme.colorScheme.primaryContainer
                                        FastStatus.MISSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        FastStatus.EXEMPT -> MaterialTheme.colorScheme.surfaceVariant
                                        FastStatus.NOT_RECORDED -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                                .border(
                                    if (isToday) 2.dp else 1.dp,
                                    if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onCycleFastStatus(date)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNum",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (status) {
                                        FastStatus.FASTED -> "✓"
                                        FastStatus.MISSED -> "✗"
                                        FastStatus.EXEMPT -> "—"
                                        FastStatus.NOT_RECORDED -> "○"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (status) {
                                        FastStatus.FASTED -> MaterialTheme.colorScheme.primary
                                        FastStatus.MISSED -> MaterialTheme.colorScheme.error
                                        FastStatus.EXEMPT -> MaterialTheme.colorScheme.onSurfaceVariant
                                        FastStatus.NOT_RECORDED -> MaterialTheme.colorScheme.outline
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Qadha Make-up Counter
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "QADHA MAKE-UP FASTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Completed: ${progress.qadhaMadeUpCount} made up",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            if (progress.qadhaMadeUpCount > 0) {
                                onUpdateQadhaMadeUp(progress.qadhaMadeUpCount - 1)
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${progress.qadhaMadeUpCount}",
                        style = TabularClockStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )

                    OutlinedButton(
                        onClick = {
                            onUpdateQadhaMadeUp(progress.qadhaMadeUpCount + 1)
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Authentic verified Duas for Ramadan, Iftar, Suhoor, Laylat al-Qadr, and Ashras.
 */
@Composable
private fun DuasSection(
    selectedCategory: RamadanDuaCategory,
    onSelectCategory: (RamadanDuaCategory) -> Unit
) {
    Column {
        // Category Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(RamadanDuaCategory.entries.toTypedArray()) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectCategory(cat) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat.displayName,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val filteredDuas = if (selectedCategory == RamadanDuaCategory.ALL) {
            RamadanDuasStore.allDuas
        } else {
            RamadanDuasStore.allDuas.filter { it.category == selectedCategory }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            filteredDuas.forEach { dua ->
                DuaCard(dua = dua)
            }
        }
    }
}

/**
 * Card presenting an individual supplication with Arabic script, transliteration, and citation.
 */
@Composable
private fun DuaCard(dua: RamadanDua) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dua.title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = dua.category.displayName,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dua.contextNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dua.contextNote,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text
            Text(
                text = dua.arabicText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Transliteration
            Text(
                text = dua.transliteration,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Translation
            Text(
                text = "\"${dua.translation}\"",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reference
            Text(
                text = dua.reference,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 30-Day computed Ramadan timetable computed offline via [PrayerCalculationEngine].
 */
@Composable
private fun TimetableSection(
    state: PrayerDashboardUiState,
    calculationEngine: PrayerCalculationEngine
) {
    val hijriYear = state.islamicDateInfo.hijriYear
    val startDate = remember(hijriYear) { IslamicCalendarHelper.getRamadanStartDate(hijriYear) }
    val timeZone = TimeZone.currentSystemDefault()
    val coordinates = remember(state.userPreferences.location) {
        Coordinates(state.userPreferences.location.latitude, state.userPreferences.location.longitude)
    }
    val params = remember(state.userPreferences) {
        calculationEngine.createParameters(
            method = state.userPreferences.calculationMethod.toAdhanMethod(),
            madhab = state.userPreferences.madhab.toAdhanMadhab(),
            highLatitudeRule = state.userPreferences.highLatitudeRule.toAdhanRule()
        )
    }

    val scheduleDays = remember(startDate, coordinates, params) {
        (0 until 30).map { dayIndex ->
            val date = startDate.plus(dayIndex, DateTimeUnit.DAY)
            val dailyTimes = calculationEngine.computeDailyTimes(
                date = date,
                coordinates = coordinates,
                parameters = params,
                manualOffsets = state.userPreferences.manualMinuteOffsets
            )

            val durationMillis = (dailyTimes.maghrib - dailyTimes.fajr).coerceAtLeast(0L)
            val hours = durationMillis / 3_600_000L
            val minutes = (durationMillis % 3_600_000L) / 60_000L

            RamadanDaySchedule(
                dayNumber = dayIndex + 1,
                gregorianDate = date,
                suhoorCutoffMillis = dailyTimes.fajr,
                sunriseMillis = dailyTimes.sunrise,
                iftarMillis = dailyTimes.maghrib,
                formattedDate = "${date.dayOfMonth} ${date.month.name.take(3)}",
                formattedSuhoor = PrayerViewModel.formatTimeOfDay(dailyTimes.fajr, timeZone),
                formattedIftar = PrayerViewModel.formatTimeOfDay(dailyTimes.maghrib, timeZone),
                formattedFastDuration = "${hours}h ${minutes}m"
            )
        }
    }

    Column {
        Text(
            text = "30-DAY RAMADAN SCHEDULE (${state.locationName})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Table Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Day", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                Text(text = "Date", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                Text(text = "Suhoor (Fajr)", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text(text = "Iftar (Maghrib)", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text(text = "Duration", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Table Rows
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            scheduleDays.forEach { schedule ->
                val isToday = schedule.gregorianDate == state.islamicDateInfo.gregorianDate
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            if (isToday) 1.dp else 0.dp,
                            if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isToday) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${schedule.dayNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.8f)
                        )
                        Text(
                            text = schedule.formattedDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = schedule.formattedSuhoor,
                            style = TabularClockStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = schedule.formattedIftar,
                            style = TabularClockStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = schedule.formattedFastDuration,
                            style = TabularClockStyle.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1.0f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section presenting Eid al-Fitr & Eid al-Adha greetings, Takbirat counter, and Sunnahs.
 */
@Composable
private fun EidSection() {
    var takbiratCount by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Eid Greetings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "CELEBRATIONS OF THE TWO MAJOR EIDS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تَقَبَّلَ اللَّهُ مِنَّا وَمِنْكُمْ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Taqabbal Allahu Minna wa Minkum",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"May Allah accept good deeds from us and from you.\"",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Interactive Takbirat Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    takbiratCount++
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THE TAKBIRAT OF EID (TAP ANYWHERE TO COUNT)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Count: $takbiratCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ لَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ وَلِلَّهِ الْحَمْدُ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Allahu Akbar, Allahu Akbar, La ilaha illallah, Wallahu Akbar, Allahu Akbar, wa lillahil-hamd",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"Allah is the greatest, Allah is the greatest, there is no deity worthy of worship except Allah. Allah is the greatest, Allah is the greatest, and to Allah belongs all praise.\"",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sunnahs of Eid al-Fitr Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SUNNAH PRACTICES OF EID AL-FITR (1ST SHAWWAL)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                EidGuide.eidAlFitrSunnahs.forEachIndexed { index, sunnah ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${index + 1}.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(text = sunnah.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = sunnah.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = sunnah.hadithReference, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        }
                    }
                    if (index < EidGuide.eidAlFitrSunnahs.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Sunnahs of Eid al-Adha Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SUNNAH PRACTICES OF EID AL-ADHA (10TH DHUL-HIJJAH)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                EidGuide.eidAlAdhaSunnahs.forEachIndexed { index, sunnah ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${index + 1}.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(text = sunnah.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = sunnah.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = sunnah.hadithReference, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        }
                    }
                    if (index < EidGuide.eidAlAdhaSunnahs.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * Settings section for Suhoor wake-up alarm and personalized Ramadan/Eid notifications.
 */
@Composable
private fun AlertsSection(
    settings: RamadanSettings,
    onUpdateSuhoorAlarm: (Boolean, Int?) -> Unit,
    onUpdateIftarAlert: (Boolean) -> Unit,
    onUpdateEidAlert: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Suhoor Pre-Fajr Wake Up Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pre-Suhoor Wake-Up Alarm",
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Exact alarm before Fajr dawn to eat Suhoor & make Niyyah",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.suhoorAlarmEnabled,
                        onCheckedChange = { onUpdateSuhoorAlarm(it, null) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                if (settings.suhoorAlarmEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ALARM TIMING BEFORE FAJR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 40, 50, 60).forEach { mins ->
                            val isSelected = settings.suhoorAlarmMinutesBefore == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdateSuhoorAlarm(true, mins) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Iftar Notification Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Iftar Maghrib Notification",
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = "Special notification at Maghrib featuring the authentic Iftar Dua",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.iftarNotificationEnabled,
                    onCheckedChange = onUpdateIftarAlert,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        // Eid Morning Greeting Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eid Celebrations & Greetings",
                        style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = "Morning celebration announcements on Eid al-Fitr & Eid al-Adha",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.eidNotificationEnabled,
                    onCheckedChange = onUpdateEidAlert,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}
