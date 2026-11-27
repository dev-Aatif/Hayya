package dev.hayya.core.data.hadith

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HadithDatabaseTest {

    @Test
    fun buildSafeFtsMatchQuery_quotesReservedKeywordsAndStripsOperators() {
        assertThat(HadithDatabase.buildSafeFtsMatchQuery("   ")).isNull()
        assertThat(HadithDatabase.buildSafeFtsMatchQuery("prayer OR fasting AND charity NOT\"*():-^"))
            .isEqualTo("prayer* \"OR\" fasting* \"AND\" charity* \"NOT\"")
        assertThat(HadithDatabase.buildSafeFtsMatchQuery("NEAR (salah)"))
            .isEqualTo("\"NEAR\" salah*")
        assertThat(HadithDatabase.buildSafeFtsMatchQuery("الصلاة"))
            .isEqualTo("الصلاة*")
    }

    @Test
    fun escapeLikeWildcards_escapesBackslashPercentAndUnderscore() {
        assertThat(HadithDatabase.escapeLikeWildcards("100%_test\\path"))
            .isEqualTo("100\\%\\_test\\\\path")
    }
}
