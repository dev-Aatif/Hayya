package dev.hayya.core.data.hadith

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.hadith.CanonicalHadithBooks
import dev.hayya.core.domain.hadith.FawazHadithJsonParser
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.system.update.UpdateHttpTransport
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class FawazHadithSyncClientTest {

    @Test
    fun parseEditionsIndexAndEditionBook_extractsExpectedFields() {
        val parsedIndex = FawazHadithJsonParser.parseEditionsIndex(SAMPLE_EDITIONS_JSON)
        assertThat(parsedIndex).containsKey("bukhari")
        assertThat(parsedIndex["bukhari"]).hasSize(3)
        assertThat(parsedIndex["bukhari"]!!.first().downloadUrl)
            .isEqualTo("https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.min.json")

        val parsedBook = FawazHadithJsonParser.parseEditionBook(SAMPLE_ENG_BUKHARI_JSON)
        assertThat(parsedBook).isNotNull()
        assertThat(parsedBook!!.sections["2"]).isEqualTo("Belief")
        assertThat(parsedBook.sections["34"]).isEqualTo("Sales and Trade")
        assertThat(parsedBook.hadiths).hasSize(2)
        assertThat(parsedBook.hadiths[0].hadithNumber).isEqualTo("1")
        assertThat(parsedBook.hadiths[0].sectionKey).isEqualTo("2")
    }

    @Test
    fun downloadBook_mergesArabicEnglishUrduAndClassifiesSections() = runTest {
        val transport = FakeHttpTransport(
            mapOf(
                FawazHadithSyncClient.DEFAULT_EDITIONS_URL to SAMPLE_EDITIONS_JSON,
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.min.json" to SAMPLE_ARA_BUKHARI_JSON,
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari.min.json" to SAMPLE_ENG_BUKHARI_JSON,
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/urd-bukhari.min.json" to SAMPLE_URD_BUKHARI_JSON
            )
        )

        val client = FawazHadithSyncClient(httpTransport = transport)
        val bukhari = CanonicalHadithBooks.catalog.first { it.bookId == "bukhari" }
        val stages = mutableListOf<String>()
        val result = client.downloadBook(bukhari) { stages.add(it) }

        assertThat(result.isSuccess).isTrue()
        val downloaded = result.getOrThrow()
        assertThat(downloaded.downloadedLanguages).containsExactly("AR", "EN", "UR").inOrder()
        assertThat(downloaded.entries).hasSize(2)
        assertThat(stages).isNotEmpty()

        val beliefHadith = downloaded.entries[0]
        assertThat(beliefHadith.id).isEqualTo("fw_bukhari_1")
        assertThat(beliefHadith.bookId).isEqualTo("bukhari")
        assertThat(beliefHadith.pillar).isEqualTo(HadithPillar.CREED)
        assertThat(beliefHadith.isPriority).isTrue()
        assertThat(beliefHadith.grade).isEqualTo("Sahih")
        assertThat(beliefHadith.arabic.matn).contains("إنما الأعمال")
        assertThat(beliefHadith.translations[HadithLanguage.ENGLISH]?.matn).contains("intentions")
        assertThat(beliefHadith.translations[HadithLanguage.URDU]?.matn).contains("نیت")

        val tradeHadith = downloaded.entries[1]
        assertThat(tradeHadith.id).isEqualTo("fw_bukhari_2050")
        assertThat(tradeHadith.isPriority).isFalse()
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

    private companion object {
        val SAMPLE_EDITIONS_JSON = """
            {
              "bukhari": {
                "name": "Sahih al Bukhari",
                "collection": [
                  {
                    "name": "ara-bukhari",
                    "book": "bukhari",
                    "language": "Arabic",
                    "link": "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.json",
                    "linkmin": "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.min.json"
                  },
                  {
                    "name": "eng-bukhari",
                    "book": "bukhari",
                    "language": "English",
                    "link": "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari.json",
                    "linkmin": "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-bukhari.min.json"
                  },
                  {
                    "name": "urd-bukhari",
                    "book": "bukhari",
                    "language": "Urdu",
                    "link": "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/urd-bukhari.json",
                    "linkmin": "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/urd-bukhari.min.json"
                  }
                ]
              }
            }
        """.trimIndent()

        val SAMPLE_ARA_BUKHARI_JSON = """
            {
              "metadata": {"name": "Sahih al Bukhari", "sections": {"2": "كتاب الإيمان", "34": "كتاب البيوع"}},
              "hadiths": [
                {"hadithnumber": 1, "text": "إنما الأعمال بالنيات", "grades": [], "reference": {"book": 2, "hadith": 1}},
                {"hadithnumber": 2050, "text": "البيعان بالخيار", "grades": [], "reference": {"book": 34, "hadith": 1}}
              ]
            }
        """.trimIndent()

        val SAMPLE_ENG_BUKHARI_JSON = """
            {
              "metadata": {"name": "Sahih al Bukhari", "sections": {"2": "Belief", "34": "Sales and Trade"}},
              "hadiths": [
                {"hadithnumber": 1, "text": "Actions are only by intentions.", "grades": [], "reference": {"book": 2, "hadith": 1}},
                {"hadithnumber": 2050, "text": "The buyer and seller have the option to cancel.", "grades": [], "reference": {"book": 34, "hadith": 1}}
              ]
            }
        """.trimIndent()

        val SAMPLE_URD_BUKHARI_JSON = """
            {
              "metadata": {"name": "Sahih al Bukhari", "sections": {"2": "ایمان کا بیان", "34": "خرید و فروخت"}},
              "hadiths": [
                {"hadithnumber": 1, "text": "اعمال کا دارومدار نیت پر ہے", "grades": [], "reference": {"book": 2, "hadith": 1}},
                {"hadithnumber": 2050, "text": "خریدنے اور بیچنے والے کو اختیار ہے", "grades": [], "reference": {"book": 34, "hadith": 1}}
              ]
            }
        """.trimIndent()
    }
}
