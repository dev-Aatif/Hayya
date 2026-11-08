package dev.hayya.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PostAdhanDuaTest {

    @Test
    fun postAdhanDua_containsAuthenticArabicTextAndBukhariReference() {
        assertThat(PostAdhanDua.TITLE).isEqualTo("DUA AFTER ADHAN")
        assertThat(PostAdhanDua.REFERENCE).isEqualTo("Sahih al-Bukhari 614")

        // Verify Arabic text contains key phrases
        assertThat(PostAdhanDua.ARABIC_TEXT).contains("اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ")
        assertThat(PostAdhanDua.ARABIC_TEXT).contains("آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ")
        assertThat(PostAdhanDua.ARABIC_TEXT).contains("مَقَامًا مَحْمُودًا")

        // Verify English transliteration
        assertThat(PostAdhanDua.TRANSLITERATION).contains("Allāhumma Rabba hādhihid-da‘wati-ttāmmati")
        assertThat(PostAdhanDua.TRANSLITERATION).contains("wasīlata wal-faḍīlah")

        // Verify English translation
        assertThat(PostAdhanDua.TRANSLATION).contains("O Allah, Lord of this perfect call")
        assertThat(PostAdhanDua.TRANSLATION).contains("Wasilah")

        // Verify prophetic intercession virtue context
        assertThat(PostAdhanDua.VIRTUE).contains("my intercession is guaranteed for him on the Day of Resurrection")
    }
}
