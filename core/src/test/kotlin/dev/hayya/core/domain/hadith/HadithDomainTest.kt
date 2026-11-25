package dev.hayya.core.domain.hadith

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.data.hadith.HadithStarterPack
import kotlinx.datetime.LocalDate
import org.junit.Test

class HadithDomainTest {

    @Test
    fun priorityTopicFilter_acceptsSpiritualAndExcludesJudicialLaw() {
        assertThat(PriorityTopicFilter.classifyCategory("1", "The Noble Qur'an and Qur'anic Sciences"))
            .isEqualTo(HadithPillar.TAFSIR)
        assertThat(PriorityTopicFilter.classifyCategory("3", "The Creed"))
            .isEqualTo(HadithPillar.CREED)
        assertThat(PriorityTopicFilter.classifyCategory("40", "Jurisprudence of Acts of Worship", parentId = "4"))
            .isEqualTo(HadithPillar.WORSHIP)
        assertThat(PriorityTopicFilter.classifyCategory("5", "Virtues and Manners"))
            .isEqualTo(HadithPillar.MANNERS)
        assertThat(PriorityTopicFilter.classifyCategory("55", "Heart-Softening Virtues & Repentance", parentId = "5"))
            .isEqualTo(HadithPillar.VIRTUES)

        // Excluded legal/penal/judicial categories
        assertThat(PriorityTopicFilter.classifyCategory("41", "Prescribed Punishments", parentId = "4")).isNull()
        assertThat(PriorityTopicFilter.classifyCategory("42", "Crimes", parentId = "4")).isNull()
        assertThat(PriorityTopicFilter.classifyCategory("43", "Judiciary", parentId = "4")).isNull()
        assertThat(PriorityTopicFilter.classifyCategory("44", "Jurisprudence of Transactions", parentId = "4")).isNull()
        assertThat(PriorityTopicFilter.classifyCategory("45", "Principles of Islamic Jurisprudence", parentId = "4")).isNull()
    }

    @Test
    fun hadeethEncParser_parsesCategoriesListAndDetail() {
        val catJson = """
            [
              {"id": "3", "title": "The Creed", "hadeeths_count": "441", "parent_id": null},
              {"id": "41", "title": "Prescribed Punishments", "hadeeths_count": "25", "parent_id": "4"}
            ]
        """.trimIndent()
        val categories = HadeethEncJsonParser.parseCategories(catJson)
        assertThat(categories).hasSize(2)
        assertThat(categories[0].id).isEqualTo("3")
        assertThat(categories[0].hadeethsCount).isEqualTo(441)
        assertThat(categories[0].parentId).isNull()
        assertThat(categories[1].parentId).isEqualTo("4")

        val listJson = """
            {
              "data": [
                {"id": "1751", "title": "Actions by intentions", "translations": ["ar", "en", "ur", "sd"]}
              ],
              "meta": {"current_page": "1", "last_page": 5, "total_items": 100, "per_page": "20"}
            }
        """.trimIndent()
        val page = HadeethEncJsonParser.parseListPage(listJson)
        assertThat(page).isNotNull()
        assertThat(page!!.items).hasSize(1)
        assertThat(page.items[0].translations).containsExactly("ar", "en", "ur", "sd").inOrder()
        assertThat(page.lastPage).isEqualTo(5)

        val detailJson = """
            {
              "id": "1751",
              "title": "Actions are by intentions",
              "hadeeth": "Actions are only by intentions...",
              "attribution": "Sahih al-Bukhari",
              "grade": "Sahih",
              "explanation": "Ikhlas is required for acceptance.",
              "hints": ["Purify intention", "Avoid showing off"]
            }
        """.trimIndent()
        val detail = HadeethEncJsonParser.parseHadithDetail(detailJson)
        assertThat(detail).isNotNull()
        assertThat(detail!!.attribution).isEqualTo("Sahih al-Bukhari")
        assertThat(detail.grade).isEqualTo("Sahih")
        assertThat(detail.hints).containsExactly("Purify intention", "Avoid showing off").inOrder()
    }

    @Test
    fun hadithEntry_resolveTranslation_fallsBackGracefullyFromSindhiToUrdu() {
        val entry = HadithEntry(
            id = "he_1",
            pillar = HadithPillar.WORSHIP,
            categoryId = "40",
            categoryTitle = "Prayer",
            attribution = "Sahih Muslim",
            grade = "Sahih",
            arabic = HadithTextVariant("عنوان", "نص عربي"),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant("Title", "English text"),
                HadithLanguage.URDU to HadithTextVariant("عنوان", "اردو متن")
            )
        )

        val directUrdu = entry.resolveTranslation(HadithLanguage.URDU)
        assertThat(directUrdu.language).isEqualTo(HadithLanguage.URDU)
        assertThat(directUrdu.isFallback).isFalse()

        val fallbackSindhi = entry.resolveTranslation(HadithLanguage.SINDHI)
        assertThat(fallbackSindhi.language).isEqualTo(HadithLanguage.URDU)
        assertThat(fallbackSindhi.isFallback).isTrue()
        assertThat(fallbackSindhi.variant.matn).isEqualTo("اردو متن")
    }

    @Test
    fun dailyHadithSelector_isDeterministicAndPrefersUnread() {
        val entries = HadithStarterPack.entries.take(4).mapIndexed { index, item ->
            item.copy(isRead = index < 3)
        }
        val today = LocalDate(2026, 11, 26)
        val picked = DailyHadithSelector.selectForDate(entries, today)
        assertThat(picked?.id).isEqualTo(entries[3].id)

        val sameAgain = DailyHadithSelector.selectForDate(entries, today)
        assertThat(sameAgain).isEqualTo(picked)
    }

    @Test
    fun starterPack_coversAllFivePillarsWithMultilingualText() {
        val entries = HadithStarterPack.entries
        assertThat(entries.map { it.id }).containsNoDuplicates()
        assertThat(entries.map { it.pillar }.toSet()).containsExactlyElementsIn(HadithPillar.entries)

        entries.forEach { entry ->
            assertThat(entry.arabic.matn).isNotEmpty()
            assertThat(entry.attribution).isNotEmpty()
            assertThat(entry.grade).isNotEmpty()
            HadithLanguage.entries.forEach { lang ->
                val variant = entry.translations[lang]
                assertThat(variant).isNotNull()
                assertThat(variant!!.matn).isNotEmpty()
            }
        }
    }
}
