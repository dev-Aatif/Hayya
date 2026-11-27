package dev.hayya.core.domain.hadith

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.data.hadith.HadithStarterPack
import org.junit.Test

class HadithIsnadFormatterTest {

    @Test
    fun formatTranslation_shortensLongBukhariUrduChainToKeyNarrators() {
        val raw = "کو حمیدی نے یہ حدیث بیان کی، انہوں نے کہا کہ ہم کو سفیان نے یہ حدیث بیان کی، " +
            "وہ کہتے ہیں ہم کو یحییٰ بن سعید انصاری نے یہ حدیث بیان کی، انہوں نے کہا کہ مجھے یہ حدیث " +
            "محمد بن ابراہیم تیمی سے حاصل ہوئی۔ انہوں نے اس حدیث کو علقمہ بن وقاص لیثی سے سنا، " +
            "ان کا بیان ہے کہ میں نے مسجد نبوی میں منبر رسول صلی اللہ علیہ وسلم پر عمر بن خطاب رضی اللہ عنہ " +
            "کی زبان سے سنا، وہ فرما رہے تھے کہ میں نے جناب رسول اللہ صلی اللہ علیہ وسلم سے سنا " +
            "آپ صلی اللہ علیہ وسلم فرما رہے تھے کہ تمام اعمال کا دارومدار نیت پر ہے اور ہر عمل کا نتیجہ ہر انسان کو اس کی نیت کے مطابق ہی ملے گا۔"

        val formatted = HadithIsnadFormatter.formatTranslation(raw, HadithLanguage.URDU)

        assertThat(formatted).contains("سفیان")
        assertThat(formatted).contains("علقمہ بن وقاص لیثی")
        assertThat(formatted).contains("عمر بن خطاب رضی اللہ عنہ")
        assertThat(formatted).doesNotContain("حمیدی")
        assertThat(formatted).doesNotContain("یحییٰ بن سعید انصاری")
        assertThat(formatted).contains("تمام اعمال کا دارومدار نیت پر ہے")
        assertThat(formatted.length).isLessThan(raw.length - 100)
    }

    @Test
    fun formatTranslation_handlesZwnjInMuslimAndMultipleHonorifics() {
        val bukhari528 = "ہم سے ابراہیم بن حمزہ نے بیان کیا، کہا ہم سے عبدالعزیز بن ابی حازم اور عبدالعزیز بن محمد دراوردی نے " +
            "یزید بن عبداللہ کی روایت سے، انہوں نے محمد بن ابراہیم تیمی سے، انہوں نے ابوسلمہ بن عبدالرحمٰن بن عوف رضی اللہ عنہ سے، " +
            "انہوں نے ابوہریرہ رضی اللہ عنہ سے کہ انہوں نے رسول اللہ صلی اللہ علیہ وسلم سے سنا آپ صلی اللہ علیہ وسلم فرماتے تھے کہ " +
            "اگر کسی شخص کے دروازے پر نہر جاری ہو اور وہ روزانہ اس میں پانچ پانچ دفعہ نہائے تو کیا اس کے بدن پر کچھ بھی میل باقی رہ سکتا ہے؟"

        val formatted = HadithIsnadFormatter.formatTranslation(bukhari528, HadithLanguage.URDU)

        assertThat(formatted).contains("ابوہریرہ رضی اللہ عنہ سے روایت ہے:")
        assertThat(formatted).contains("اگر کسی شخص کے دروازے پر نہر جاری ہو")
        assertThat(formatted).doesNotContain("ابراہیم بن حمزہ")
        assertThat(formatted).doesNotContain("عبدالعزیز")
    }

    @Test
    fun formatTranslation_skipsKinshipPronounAndKeepsImamMalikInBukhari2() {
        val bukhari2 = "ہم کو عبداللہ بن یوسف نے حدیث بیان کی، ان کو مالک نے ہشام بن عروہ کی روایت سے خبر دی، " +
            "انہوں نے اپنے والد سے نقل کی، انہوں نے ام المؤمنین عائشہ صدیقہ رضی اللہ عنہا سے نقل کی " +
            "آپ نے فرمایا کہ ایک شخص حارث بن ہشام نامی نے نبی کریم صلی اللہ علیہ وسلم سے سوال کیا تھا کہ یا رسول اللہ! آپ پر وحی کیسے نازل ہوتی ہے؟"

        val formatted = HadithIsnadFormatter.formatTranslation(bukhari2, HadithLanguage.URDU)

        assertThat(formatted).contains("مالک")
        assertThat(formatted).contains("ہشام بن عروہ")
        assertThat(formatted).contains("ام المؤمنین عائشہ صدیقہ رضی اللہ عنہا سے روایت ہے:")
        assertThat(formatted).doesNotContain("عبداللہ بن یوسف")
        assertThat(formatted).doesNotContain("اپنے والد")
        assertThat(formatted).contains("آپ نے فرمایا کہ ایک شخص حارث بن ہشام نامی")
    }

    @Test
    fun formatTranslation_condensesLongSindhiChainToo() {
        val longSindhi = "اسان کي عبدالله بن يوسف بيان ڪيو، هنن چيو ته اسان کي امام مالک خبر ڏني، " +
            "هنن نافع کان ۽ هنن عبدالله بن عمر رضي الله عنهما کان روايت ڪيو ته پاڻ سڳورن ﷺ فرمايو ته " +
            "اسلام جي بنياد پنجن شين تي رکيل آهي: لا اله الا الله محمد رسول الله جي شاهدي ڏيڻ، نماز قائم ڪرڻ، زڪوات ادا ڪرڻ."

        val formatted = HadithIsnadFormatter.formatTranslation(longSindhi, HadithLanguage.SINDHI)

        assertThat(formatted).contains("امام مالک")
        assertThat(formatted).contains("نافع")
        assertThat(formatted).contains("عبدالله بن عمر رضي الله عنهما کان روايت آهي:")
        assertThat(formatted).doesNotContain("عبدالله بن يوسف")
        assertThat(formatted).contains("اسلام جي بنياد پنجن شين تي")
    }

    @Test
    fun formatTranslation_leavesShortChainsStarterPackEnglishAndProseUntouched() {
        val shortAbuDawud = "مغیرہ بن شعبہ رضی اللہ عنہ کہتے ہیں کہ نبی اکرم صلی اللہ علیہ وسلم جب قضائے حاجت کے لیے جاتے تو دور تشریف لے جاتے تھے۔"
        val shortMuslim500 = "حضرت انس ‌رضی ‌اللہ ‌عنہ ‌ ‌ سے روایت ہے کہ ایک آدمی نے پوچھا : اے اللہ کے رسول ! میرا باپ کہاں ہے ؟ آپ نے فرمایا : ’’ آگ میں ۔ ‘ ‘"
        val muslimIntroProse = "جان تو! اللہ تجھ کو توفیق دے جو شخص صحیح اور ضعیف حدیث میں تمیز کرنے کی قدرت رکھتا ہو اور ثقہ راویوں کو پہچانتا ہو " +
            "اس پر واجب ہے کہ نہ روایت کرے مگر اس حدیث کو جس کے اصل کی صحت ہو اور اس کے نقل کرنے والے وہ لوگ ہوں جن کا عیب فاش نہ ہوا ہو۔"
        val englishText = "Narrated 'Umar bin Al-Khattab: I heard Allah's Messenger (ﷺ) saying, \"Actions are judged by intentions.\""

        assertThat(HadithIsnadFormatter.formatTranslation(shortAbuDawud, HadithLanguage.URDU)).isEqualTo(shortAbuDawud)
        assertThat(HadithIsnadFormatter.formatTranslation(shortMuslim500, HadithLanguage.URDU)).isEqualTo(shortMuslim500)
        assertThat(HadithIsnadFormatter.formatTranslation(muslimIntroProse, HadithLanguage.URDU)).isEqualTo(muslimIntroProse)
        assertThat(HadithIsnadFormatter.formatTranslation(englishText, HadithLanguage.ENGLISH)).isEqualTo(englishText)

        for (entry in HadithStarterPack.entries) {
            for ((lang, variant) in entry.translations) {
                assertThat(HadithIsnadFormatter.formatTranslation(variant.matn, lang)).isEqualTo(variant.matn)
            }
            assertThat(HadithIsnadFormatter.formatArabic(entry.arabic.matn)).isEqualTo(entry.arabic.matn)
        }
    }

    @Test
    fun formatArabic_shortensLongBukhariChainToLastTwoAnLinks() {
        val araBukhari528 = "حَدَّثَنَا إِبْرَاهِيمُ بْنُ حَمْزَةَ، قَالَ حَدَّثَنِي ابْنُ أَبِي حَازِمٍ، وَالدَّرَاوَرْدِيُّ، " +
            "عَنْ يَزِيدَ، عَنْ مُحَمَّدِ بْنِ إِبْرَاهِيمَ، عَنْ أَبِي سَلَمَةَ بْنِ عَبْدِ الرَّحْمَنِ، عَنْ أَبِي هُرَيْرَةَ، " +
            "أَنَّهُ سَمِعَ رَسُولَ اللَّهِ صلى الله عليه وسلم يَقُولُ \" أَرَأَيْتُمْ لَوْ أَنَّ نَهَرًا بِبَابِ أَحَدِكُمْ \""

        val formatted = HadithIsnadFormatter.formatArabic(araBukhari528)

        assertThat(formatted).startsWith("عَنْ أَبِي سَلَمَةَ بْنِ عَبْدِ الرَّحْمَنِ، عَنْ أَبِي هُرَيْرَةَ")
        assertThat(formatted).doesNotContain("إِبْرَاهِيمُ بْنُ حَمْزَةَ")
        assertThat(formatted).contains("أَرَأَيْتُمْ لَوْ أَنَّ نَهَرًا")
    }
}
