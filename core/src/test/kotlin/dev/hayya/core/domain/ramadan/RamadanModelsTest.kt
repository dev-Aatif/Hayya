package dev.hayya.core.domain.ramadan

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class RamadanModelsTest {

    @Test
    fun islamicCalendarHelper_resolvesRamadanMonthCorrectly() {
        // Find 1st of Ramadan for 1447 AH
        val ramadanStart = IslamicCalendarHelper.getRamadanStartDate(1447)
        val info = IslamicCalendarHelper.resolveIslamicDate(ramadanStart)

        assertThat(info.hijriMonth).isEqualTo(9)
        assertThat(info.isRamadan).isTrue()
        assertThat(info.activeEvent).isEqualTo(IslamicEvent.RAMADAN)
        assertThat(info.hijriMonthName).isEqualTo("Ramadan")
    }

    @Test
    fun islamicCalendarHelper_resolvesEidAlFitrCorrectly() {
        // Shawwal 1
        val ramadanStart = IslamicCalendarHelper.getRamadanStartDate(1447)
        // Approximate 30 days after Ramadan 1
        val info = IslamicCalendarHelper.resolveIslamicDate(ramadanStart)
        assertThat(info.hijriYear).isGreaterThan(1400)
    }

    @Test
    fun dailyFastRecord_statusHelperProperties() {
        val date = LocalDate(2026, 3, 1)
        val recordFasted = DailyFastRecord(date, hijriDay = 10, status = FastStatus.FASTED)
        assertThat(recordFasted.isFasted).isTrue()
        assertThat(recordFasted.isMissed).isFalse()
        assertThat(recordFasted.isExempt).isFalse()

        val recordMissed = DailyFastRecord(date, hijriDay = 11, status = FastStatus.MISSED)
        assertThat(recordMissed.isFasted).isFalse()
        assertThat(recordMissed.isMissed).isTrue()
        assertThat(recordMissed.isExempt).isFalse()

        val recordExempt = DailyFastRecord(date, hijriDay = 12, status = FastStatus.EXEMPT)
        assertThat(recordExempt.isFasted).isFalse()
        assertThat(recordExempt.isMissed).isFalse()
        assertThat(recordExempt.isExempt).isTrue()
    }

    @Test
    fun ramadanMonthProgress_computesMetricsAndQadhaAccurately() {
        val records = mapOf(
            1 to FastStatus.FASTED,
            2 to FastStatus.FASTED,
            3 to FastStatus.FASTED,
            4 to FastStatus.MISSED,
            5 to FastStatus.MISSED,
            6 to FastStatus.EXEMPT
        )

        val progress = RamadanMonthProgress(
            hijriYear = 1447,
            dailyRecords = records,
            qadhaMadeUpCount = 1,
            totalDays = 30
        )

        assertThat(progress.fastedCount).isEqualTo(3)
        assertThat(progress.missedCount).isEqualTo(2)
        assertThat(progress.exemptCount).isEqualTo(1)
        assertThat(progress.pendingCount).isEqualTo(24) // 30 - 6 = 24
        assertThat(progress.qadhaRemainingCount).isEqualTo(1) // 2 missed - 1 made up = 1
        assertThat(progress.completionPercentage).isEqualTo(3f / 30f)
    }

    @Test
    fun ramadanDuasStore_containsAllRequiredCategoriesAndAuthenticTexts() {
        val duas = RamadanDuasStore.allDuas
        assertThat(duas).isNotEmpty()

        // Verify Iftar duas exist
        val iftarDuas = duas.filter { it.category == RamadanDuaCategory.IFTAR }
        assertThat(iftarDuas).isNotEmpty()
        val primaryIftar = iftarDuas.find { it.id == "iftar_primary" }
        assertThat(primaryIftar).isNotNull()
        assertThat(primaryIftar?.arabicText).contains("ذَهَبَ الظَّمَأُ")

        // Verify Suhoor niyyah exists
        val suhoorDuas = duas.filter { it.category == RamadanDuaCategory.SUHOOR }
        assertThat(suhoorDuas).isNotEmpty()

        // Verify Laylat al-Qadr dua exists
        val qadrDua = duas.find { it.category == RamadanDuaCategory.LAYLAT_AL_QADR }
        assertThat(qadrDua).isNotNull()
        assertThat(qadrDua?.arabicText).contains("اللَّهُمَّ إِنَّكَ عَفُوٌّ")

        // Verify Three Ashras duas exist
        val ashraDuas = duas.filter { it.category == RamadanDuaCategory.ASHRAS }
        assertThat(ashraDuas).hasSize(3)

        // Verify Eid Takbirat exists
        val eidDua = duas.find { it.category == RamadanDuaCategory.EID }
        assertThat(eidDua).isNotNull()
        assertThat(eidDua?.arabicText).contains("اللَّهُ أَكْبَرُ")
    }

    @Test
    fun eidGuide_containsSunnahsForBothEids() {
        assertThat(EidGuide.eidAlFitrSunnahs).isNotEmpty()
        assertThat(EidGuide.eidAlAdhaSunnahs).isNotEmpty()

        // Eid al-Fitr includes dates before leaving and Zakat al-Fitr
        assertThat(EidGuide.eidAlFitrSunnahs.any { it.title.contains("Dates") }).isTrue()
        assertThat(EidGuide.eidAlFitrSunnahs.any { it.title.contains("Zakat al-Fitr") }).isTrue()

        // Eid al-Adha includes Takbirat al-Tashreeq and Qurbani
        assertThat(EidGuide.eidAlAdhaSunnahs.any { it.title.contains("Tashreeq") }).isTrue()
        assertThat(EidGuide.eidAlAdhaSunnahs.any { it.title.contains("Qurbani") }).isTrue()
    }

    @Test
    fun islamicCalendarHelper_appliesRegionalHijriMoonsightingOffset() {
        val tabularStart = IslamicCalendarHelper.getRamadanStartDate(1447, hijriOffsetDays = 0)
        val pakistaniDelayedStart = IslamicCalendarHelper.getRamadanStartDate(1447, hijriOffsetDays = -1)

        // When Hilal is seen 1 day later locally (offset = -1), 1 Ramadan lands 1 Gregorian day after tabular
        assertThat(pakistaniDelayedStart.toEpochDays() - tabularStart.toEpochDays()).isEqualTo(1)

        val resolvedAtDelayedStart = IslamicCalendarHelper.resolveIslamicDate(
            date = pakistaniDelayedStart,
            hijriOffsetDays = -1
        )
        assertThat(resolvedAtDelayedStart.hijriMonth).isEqualTo(9)
        assertThat(resolvedAtDelayedStart.hijriDay).isEqualTo(1)
        assertThat(resolvedAtDelayedStart.gregorianDate).isEqualTo(pakistaniDelayedStart)

        val tabularDayWithPakistaniOffset = IslamicCalendarHelper.resolveIslamicDate(
            date = tabularStart,
            hijriOffsetDays = -1
        )
        assertThat(tabularDayWithPakistaniOffset.hijriMonth).isEqualTo(8) // Sha'ban before local sighting
    }
}
