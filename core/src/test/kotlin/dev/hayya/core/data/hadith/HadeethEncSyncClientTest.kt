package dev.hayya.core.data.hadith

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.system.update.UpdateHttpTransport
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class HadeethEncSyncClientTest {

    @Test
    fun fetchPrioritizedCategories_excludesLegalCategories() = runTest {
        val categoriesUrl = "https://hadeethenc.com/api/v1/categories/list/?language=en"
        val transport = FakeHttpTransport(
            mapOf(
                categoriesUrl to """
                    [
                      {"id": "1", "title": "The Noble Qur'an and Qur'anic Sciences", "hadeeths_count": "59", "parent_id": null},
                      {"id": "3", "title": "The Creed", "hadeeths_count": "441", "parent_id": null},
                      {"id": "40", "title": "Jurisprudence of Acts of Worship", "hadeeths_count": "832", "parent_id": "4"},
                      {"id": "41", "title": "Prescribed Punishments", "hadeeths_count": "30", "parent_id": "4"},
                      {"id": "43", "title": "Judiciary", "hadeeths_count": "18", "parent_id": "4"},
                      {"id": "5", "title": "Virtues and Manners", "hadeeths_count": "500", "parent_id": null}
                    ]
                """.trimIndent()
            )
        )

        val client = HadeethEncSyncClient(httpTransport = transport)
        val result = client.fetchPrioritizedCategories()
        assertThat(result.isSuccess).isTrue()

        val categories = result.getOrThrow()
        assertThat(categories.map { it.id }).containsExactly("1", "3", "40", "5")
    }

    @Test
    fun fetchCategoryBatch_downloadsArabicEnglishUrduAndSindhi() = runTest {
        val listUrl = "https://hadeethenc.com/api/v1/hadeeths/list/?language=en&category_id=3&page=1&per_page=20"
        val arUrl = "https://hadeethenc.com/api/v1/hadeeths/one/?id=101&language=ar"
        val enUrl = "https://hadeethenc.com/api/v1/hadeeths/one/?id=101&language=en"
        val urUrl = "https://hadeethenc.com/api/v1/hadeeths/one/?id=101&language=ur"
        val sdUrl = "https://hadeethenc.com/api/v1/hadeeths/one/?id=101&language=sd"

        val transport = FakeHttpTransport(
            mapOf(
                listUrl to """
                    {
                      "data": [
                        {"id": "101", "title": "Sincerity", "translations": ["ar", "en", "ur", "sd"]}
                      ],
                      "meta": {"current_page": "1", "last_page": 1, "total_items": 1, "per_page": "20"}
                    }
                """.trimIndent(),
                arUrl to """{"id":"101","title":"الإخلاص","hadeeth":"إنما الأعمال بالنيات","attribution":"البخاري","grade":"صحيح","explanation":"النية أساس العمل"}""",
                enUrl to """{"id":"101","title":"Sincerity","hadeeth":"Actions are by intentions","attribution":"Sahih al-Bukhari","grade":"Sahih","explanation":"Intention gives value to deeds."}""",
                urUrl to """{"id":"101","title":"اخلاص","hadeeth":"اعمال کا دارومدار نیت پر ہے","attribution":"صحیح بخاری","grade":"صحیح","explanation":"نیت عمل کی بنیاد ہے۔"}""",
                sdUrl to """{"id":"101","title":"اخلاص","hadeeth":"عملن جو دارومدار نيت تي آهي","attribution":"صحيح بخاري","grade":"صحيح","explanation":"نيت عمل جو بنياد آهي."}"""
            )
        )

        val client = HadeethEncSyncClient(httpTransport = transport)
        val category = HadithCategorySyncInfo("3", "The Creed", HadithPillar.CREED, 1, 0)
        val batch = client.fetchCategoryBatch(category)

        assertThat(batch.isSuccess).isTrue()
        val entries = batch.getOrThrow()
        assertThat(entries).hasSize(1)

        val entry = entries.first()
        assertThat(entry.id).isEqualTo("he_101")
        assertThat(entry.attribution).isEqualTo("Sahih al-Bukhari")
        assertThat(entry.translations[HadithLanguage.ENGLISH]?.matn).isEqualTo("Actions are by intentions")
        assertThat(entry.translations[HadithLanguage.URDU]?.matn).isEqualTo("اعمال کا دارومدار نیت پر ہے")
        assertThat(entry.translations[HadithLanguage.SINDHI]?.matn).isEqualTo("عملن جو دارومدار نيت تي آهي")
    }

    private class FakeHttpTransport(
        private val responses: Map<String, String>
    ) : UpdateHttpTransport {
        override suspend fun getText(url: String): Result<String> {
            val body = responses[url] ?: return Result.failure(IllegalStateException("404: $url"))
            return Result.success(body)
        }

        override suspend fun downloadToFile(
            url: String,
            destinationFile: File,
            onProgress: (Int) -> Unit
        ): Result<Unit> = Result.success(Unit)
    }
}
