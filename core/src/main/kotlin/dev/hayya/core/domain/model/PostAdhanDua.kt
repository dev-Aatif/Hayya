package dev.hayya.core.domain.model

/**
 * Authentic Hadith supplication recited after the Adhan (Call to Prayer).
 *
 * Source: Sahih al-Bukhari 614 (Book of Adhan, Chapter: The invocation at the time of the call).
 * Narrated by Jabir bin 'Abdullah (may Allah be pleased with him):
 * The Messenger of Allah ﷺ said: "Whoever says when he hears the call to prayer...
 * my intercession will be guaranteed for him on the Day of Resurrection."
 */
object PostAdhanDua {

    const val TITLE = "DUA AFTER ADHAN"

    /**
     * Authentic Arabic script with full Tashkeel / Harakat.
     */
    const val ARABIC_TEXT = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ"

    /**
     * Standard phonetic English transliteration.
     */
    const val TRANSLITERATION = "Allāhumma Rabba hādhihid-da‘wati-ttāmmati, wa-ṣṣalātil-qā’imah, āti Muḥammadanil-wasīlata wal-faḍīlah, wab‘athhu maqāmam maḥmūdanil-ladhī wa‘adtah."

    /**
     * English translation capturing canonical Islamic terminology.
     */
    const val TRANSLATION = "O Allah, Lord of this perfect call and established prayer, grant Muhammad the status of Wasilah (the highest station in Paradise) and excellence, and resurrect him to the praised station which You have promised him."

    /**
     * Canonical citation.
     */
    const val REFERENCE = "Sahih al-Bukhari 614"

    /**
     * Authentic intercession virtue.
     */
    const val VIRTUE = "The Prophet Muhammad ﷺ said: 'Whoever recites this upon hearing the Adhan, my intercession is guaranteed for him on the Day of Resurrection.'"
}
