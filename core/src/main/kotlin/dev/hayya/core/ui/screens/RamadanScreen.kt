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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.ramadan.EidGuide
import dev.hayya.core.domain.ramadan.FastStatus
import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.domain.ramadan.RamadanDaySchedule
import dev.hayya.core.domain.ramadan.RamadanDua
import dev.hayya.core.domain.ramadan.RamadanDuaCategory
import dev.hayya.core.domain.ramadan.RamadanDuasStore
import dev.hayya.core.domain.ramadan.RamadanSettings
import dev.hayya.core.ui.components.PrayerCelestialIcon
import dev.hayya.core.ui.components.SectionPageHeader
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
 * Clean, visual Ramadan & Eid Hub designed for effortless everyday use.
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
                .padding(horizontal = 20.dp)
        ) {
            SectionPageHeader(
                title = "Ramadan & Eid",
                subtitle = state.islamicDateInfo.formattedHijriDate,
                onBackClick = onBackClick,
                badgeText = if (state.islamicDateInfo.isRamadan) "Day ${state.islamicDateInfo.hijriDay}" else null
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SuhoorIftarHeroCard(
                    state = state,
                    onToggleTodayFast = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggleTodayFast()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Friendly Section Pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RamadanHubSection.entries.toTypedArray()) { section ->
                        val isSelected = section == selectedSection
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    selectedSection = section
                                }
                        ) {
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrayerCelestialIcon(prayerType = PrayerType.FAJR, size = 26.dp)
                    Column {
                        Text(
                            text = "Suhoor Ends",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = state.suhoorCutoffFormatted,
                            style = TabularClockStyle.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Iftar Time",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = state.iftarFormatted,
                            style = TabularClockStyle.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    PrayerCelestialIcon(prayerType = PrayerType.MAGHRIB, size = 26.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.isFastingCountdownToIftar) "Time Until Iftar" else "Time Until Suhoor Ends",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.fastingCountdown,
                        style = TabularCountdownStyle.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isFastedToday = state.todayFastRecord?.status == FastStatus.FASTED
            Button(
                onClick = onToggleTodayFast,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFastedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFastedToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isFastedToday) Icons.Outlined.Check else Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFastedToday) "Fasting Logged Today ✓" else "I'm Fasting Today",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun FastingTrackerSection(
    state: PrayerDashboardUiState,
    onCycleFastStatus: (LocalDate) -> Unit,
    onUpdateQadhaMadeUp: (Int) -> Unit
) {
    val progress = state.ramadanProgress
    val view = LocalView.current

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
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
                        text = "Monthly Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${progress.fastedCount} / ${progress.totalDays} Fasted",
                        style = TabularClockStyle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress.completionPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatColumn("Fasted", "${progress.fastedCount}", MaterialTheme.colorScheme.primary)
                    StatColumn("Missed", "${progress.missedCount}", MaterialTheme.colorScheme.error)
                    StatColumn("Exempt", "${progress.exemptCount}", MaterialTheme.colorScheme.onSurface)
                    StatColumn("Make-Up", "${progress.qadhaRemainingCount}", MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "30-Day Calendar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Tap any day to update",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val start = IslamicCalendarHelper.getRamadanStartDate(state.islamicDateInfo.hijriYear)
        val days = (0 until 30).map { start.plus(it, DateTimeUnit.DAY) }

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
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (status) {
                                        FastStatus.FASTED -> MaterialTheme.colorScheme.primaryContainer
                                        FastStatus.MISSED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                                        FastStatus.EXEMPT -> MaterialTheme.colorScheme.surfaceVariant
                                        FastStatus.NOT_RECORDED -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                )
                                .border(
                                    if (isToday) 2.dp else 1.dp,
                                    if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onCycleFastStatus(date)
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNum",
                                    style = TabularClockStyle.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (status) {
                                        FastStatus.FASTED -> "✓"
                                        FastStatus.MISSED -> "✗"
                                        FastStatus.EXEMPT -> "—"
                                        FastStatus.NOT_RECORDED -> "○"
                                    },
                                    fontSize = 12.sp,
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

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "✓ Fasted   •   ✗ Missed   •   — Exempt",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Make-Up Fasts Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Make-Up Fasts (Qadha)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${progress.qadhaMadeUpCount} completed",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (progress.qadhaMadeUpCount > 0) {
                                    onUpdateQadhaMadeUp(progress.qadhaMadeUpCount - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${progress.qadhaMadeUpCount}",
                        style = TabularClockStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                onUpdateQadhaMadeUp(progress.qadhaMadeUpCount + 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TabularClockStyle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun DuasSection(
    selectedCategory: RamadanDuaCategory,
    onSelectCategory: (RamadanDuaCategory) -> Unit
) {
    Column {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RamadanDuaCategory.entries.toTypedArray()) { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelectCategory(cat) }
                ) {
                    Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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

@Composable
private fun DuaCard(dua: RamadanDua) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
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
                    text = dua.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = dua.category.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (dua.contextNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dua.contextNote,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = dua.arabicText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dua.transliteration,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "“${dua.translation}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dua.reference,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

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
            text = "30-Day Timetable (${state.locationName.substringBefore(",")})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                    Text("Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    Text("Suhoor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("Iftar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("Length", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                }

                scheduleDays.forEachIndexed { idx, schedule ->
                    val isToday = schedule.gregorianDate == state.islamicDateInfo.gregorianDate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${schedule.dayNumber}",
                            style = TabularClockStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(0.7f)
                        )
                        Text(
                            text = schedule.formattedDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = schedule.formattedSuhoor,
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = schedule.formattedIftar,
                            style = TabularClockStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = schedule.formattedFastDuration,
                            style = TabularClockStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1.1f),
                            textAlign = TextAlign.End
                        )
                    }
                    if (idx < scheduleDays.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EidSection() {
    var takbiratCount by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Eid Greeting",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "تَقَبَّلَ اللَّهُ مِنَّا وَمِنْكُمْ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Taqabbal Allahu Minna wa Minkum",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "“May Allah accept good deeds from us and from you.”",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    takbiratCount++
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Takbirat of Eid (Tap to count)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Count: $takbiratCount",
                            style = TabularClockStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ لَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ وَلِلَّهِ الْحَمْدُ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Allahu Akbar, Allahu Akbar, La ilaha illallah, Wallahu Akbar, Allahu Akbar, wa lillahil-hamd",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "“Allah is the greatest, Allah is the greatest, there is no deity worthy of worship except Allah. Allah is the greatest, Allah is the greatest, and to Allah belongs all praise.”",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Sunnahs of Eid al-Fitr",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                EidGuide.eidAlFitrSunnahs.forEachIndexed { index, sunnah ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "${index + 1}.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(text = sunnah.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = sunnah.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index < EidGuide.eidAlFitrSunnahs.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Sunnahs of Eid al-Adha",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                EidGuide.eidAlAdhaSunnahs.forEachIndexed { index, sunnah ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "${index + 1}.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(text = sunnah.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = sunnah.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun AlertsSection(
    settings: RamadanSettings,
    onUpdateSuhoorAlarm: (Boolean, Int?) -> Unit,
    onUpdateIftarAlert: (Boolean) -> Unit,
    onUpdateEidAlert: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Suhoor Wake-Up Alarm",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Wake you up before Fajr to eat Suhoor",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = settings.suhoorAlarmEnabled,
                        onCheckedChange = { onUpdateSuhoorAlarm(it, null) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                if (settings.suhoorAlarmEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "How early before Fajr?",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 40, 50, 60).forEach { mins ->
                            val isSelected = settings.suhoorAlarmMinutesBefore == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onUpdateSuhoorAlarm(true, mins) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = TabularClockStyle.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Iftar Notification",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Reminds you with the Iftar Dua at Maghrib",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Switch(
                    checked = settings.iftarNotificationEnabled,
                    onCheckedChange = onUpdateIftarAlert,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}
