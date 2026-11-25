package dev.hayya.core.data.hadith

import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.domain.hadith.HadithTextVariant

/**
 * Curated offline starter collection of authentic Sahih Hadiths across Hayya's five
 * priority pillars with verified Arabic, English, Urdu, and Sindhi text so the Hadith
 * section works immediately on first launch even without internet access.
 */
object HadithStarterPack {

    const val STARTER_SEED_VERSION = 1

    val entries: List<HadithEntry> = listOf(
        HadithEntry(
            id = "starter_1",
            pillar = HadithPillar.CREED,
            categoryId = "starter_creed",
            categoryTitle = "Sincerity & Intention",
            attribution = "Sahih al-Bukhari 1, Sahih Muslim 1907",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "إنما الأعمال بالنيات",
                matn = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ فَهِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ، وَمَنْ كَانَتْ هِجْرَتُهُ لِدُنْيَا يُصِيبُهَا أَوِ امْرَأَةٍ يَنْكِحُهَا فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Actions Are Judged by Intentions",
                    matn = "Actions are only by intentions, and every person will have only what they intended. So whoever emigrated for Allah and His Messenger, his emigration is for Allah and His Messenger; and whoever emigrated for worldly gain or a woman to marry, his emigration is for whatever he emigrated for.",
                    explanation = "Sincerity (Ikhlas) turns everyday habits into worship and gives deeds their true weight before Allah."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "اعمال کا دارومدار نیتوں پر ہے",
                    matn = "اعمال کا دارومدار نیتوں پر ہے اور ہر شخص کو وہی ملے گا جس کی اس نے نیت کی۔ پس جس کی ہجرت اللہ اور اس کے رسول کے لیے ہو تو اس کی ہجرت اللہ اور اس کے رسول کی طرف شمار ہو گی، اور جس کی ہجرت دنیا کمانے یا کسی عورت سے نکاح کرنے کے لیے ہو تو اس کی ہجرت اسی چیز کے لیے ہو گی جس کے لیے اس نے ہجرت کی۔",
                    explanation = "اخلاصِ نیت عام کاموں کو بھی عبادت بنا دیتا ہے اور اللہ کے ہاں عمل کی قبولیت کی بنیاد ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "عملن جو دارومدار نيتن تي آهي",
                    matn = "عملن جو دارومدار رڳو نيتن تي آهي، ۽ هر انسان لاءِ اهو ئي آهي جنهن جي هن نيت ڪئي. پوءِ جنهن جي هجرت الله ۽ سندس رسول لاءِ هجي ته ان جي هجرت الله ۽ سندس رسول طرف شمار ٿيندي، ۽ جنهن جي هجرت دنيا حاصل ڪرڻ يا ڪنهن عورت سان نڪاح ڪرڻ لاءِ هجي ته ان جي هجرت انهيءَ طرف هوندي جنهن لاءِ هن هجرت ڪئي.",
                    explanation = "نيت جو اخلاص عام ڪمن کي به عبادت بڻائي ڇڏيندو آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_2",
            pillar = HadithPillar.WORSHIP,
            categoryId = "starter_worship",
            categoryTitle = "Five Daily Prayers",
            attribution = "Sahih al-Bukhari 528, Sahih Muslim 667",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "مثل الصلوات الخمس",
                matn = "أَرَأَيْتُمْ لَوْ أَنَّ نَهَرًا بِبَابِ أَحَدِكُمْ يَغْتَسِلُ فِيهِ كُلَّ يَوْمٍ خَمْسَ مَرَّاتٍ، هَلْ يَبْقَى مِنْ دَرَنِهِ شَيْءٌ؟ قَالُوا: لَا يَبْقَى مِنْ دَرَنِهِ شَيْءٌ. قَالَ: فَذَلِكَ مَثَلُ الصَّلَوَاتِ الْخَمْسِ يَمْحُو اللَّهُ بِهِنَّ الْخَطَايَا."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "The Parable of the Five Daily Prayers",
                    matn = "What do you think if there were a river at the door of one of you in which he bathed five times every day—would any dirt remain on him? They said: Not a trace of dirt would remain. He said: That is the likeness of the five daily prayers; through them Allah erases sins.",
                    explanation = "Consistent Salah cleanses the heart daily just as pure water washes away physical dust."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "پانچ نمازوں کی مثال",
                    matn = "بتاؤ اگر تم میں سے کسی کے دروازے پر ایک نہر ہو جس میں وہ روزانہ پانچ مرتبہ غسل کرے تو کیا اس کے جسم پر کوئی میل کچیل باقی رہے گا؟ صحابہ نے عرض کیا: کوئی میل باقی نہیں رہے گا۔ آپ ﷺ نے فرمایا: یہی مثال پانچ نمازوں کی ہے، اللہ تعالیٰ ان کے ذریعے گناہوں کو مٹا دیتا ہے۔",
                    explanation = "پابندی سے پانچ وقت کی نماز ادا کرنا دل و روح کو گناہوں سے پاک رکھتا ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "پنجن نمازن جو مثال",
                    matn = "ٻڌايو ته جيڪڏهن اوهان مان ڪنهن جي دروازي تي هڪ نهر هجي جنهن ۾ هو روزانو پنج ڀيرا غسل ڪري ته ڇا سندس جسم تي ڪا ميل باقي رهندي؟ صحابن چيو: ڪا به ميل باقي نه رهندي. پاڻ سڳورن ﷺ فرمايو: اهو ئي مثال پنجن نمازن جو آهي، جن ذريعي الله تعاليٰ گناهن کي مٽائي ڇڏي ٿو.",
                    explanation = "پابندي سان پنج وقت نماز پڙهڻ دل کي گناهن کان پاڪ رکندو آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_3",
            pillar = HadithPillar.WORSHIP,
            categoryId = "starter_worship",
            categoryTitle = "Purity & Worship",
            attribution = "Sahih Muslim 223",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "الطهور شطر الإيمان",
                matn = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ، وَسُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ تَمْلآنِ أَوْ تَمْلأُ مَا بَيْنَ السَّمَاوَاتِ وَالأَرْضِ، وَالصَّلاةُ نُورٌ، وَالصَّدَقَةُ بُرْهَانٌ، وَالصَّبْرُ ضِيَاءٌ، وَالْقُرْآنُ حُجَّةٌ لَكَ أَوْ عَلَيْكَ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Purity Is Half of Faith",
                    matn = "Purity is half of faith. Al-hamdu lillah (praise be to Allah) fills the scales, and SubhanAllah wal-hamdu lillah fill what is between the heavens and the earth. Prayer is light, charity is proof, patience is illumination, and the Qur'an is a proof for you or against you.",
                    explanation = "A concise summary of inner and outer purification: Salah brings light, Sadaqah proves sincerity, and Sabr sustains the believer."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "پاکیزگی نصف ایمان ہے",
                    matn = "پاکیزگی نصف ایمان ہے، اور الحمدللہ میزان کو بھر دیتا ہے، اور سبحان اللہ والحمدللہ آسمانوں اور زمین کے درمیان کی فضا کو بھر دیتے ہیں، اور نماز نور ہے، اور صدقہ دلیل ہے، اور صبر روشنی ہے، اور قرآن تمہارے حق میں دلیل ہے یا تمہارے خلاف۔",
                    explanation = "نماز مومن کا نور، صدقہ ایمان کی سچائی کی نشانی اور صبر آزمائش میں روشنی ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "پاڪيزگي اڌ ايمان آهي",
                    matn = "پاڪيزگي اڌ ايمان آهي، ۽ الحمدلله ميزان کي ڀري ڇڏي ٿو، ۽ سبحان الله والحمدلله آسمانن ۽ زمين جي وچ واري خلا کي ڀري ڇڏين ٿا، نماز نور آهي، صدقو دليل آهي، صبر روشني آهي، ۽ قرآن تنهنجي حق ۾ دليل آهي يا تنهنجي خلاف.",
                    explanation = "نماز نور آهي، صدقو سچائي جي نشاني آهي ۽ صبر مشڪلاتن ۾ روشني آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_4",
            pillar = HadithPillar.WORSHIP,
            categoryId = "starter_worship",
            categoryTitle = "Fasting & Taqwa",
            attribution = "Sahih al-Bukhari 1904, Sahih Muslim 1151",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "الصيام جنة",
                matn = "الصِّيَامُ جُنَّةٌ، فَلَا يَرْفُثْ وَلَا يَجْهَلْ، وَإِنِ امْرُؤٌ قَاتَلَهُ أَوْ شَاتَمَهُ فَلْيَقُلْ: إِنِّي صَائِمٌ مَرَّتَيْنِ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Fasting Is a Shield",
                    matn = "Fasting is a shield; so let the fasting person not utter foul speech or act foolishly. And if someone fights or insults him, let him say twice: Indeed, I am fasting.",
                    explanation = "True fasting protects the tongue and temper just as much as it restrains food and drink."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "روزہ ڈھال ہے",
                    matn = "روزہ ڈھال ہے، پس روزہ دار نہ فحش گوئی کرے اور نہ جہالت کی بات کرے، اور اگر کوئی اس سے لڑے یا گالی دے تو دو مرتبہ کہہ دے کہ میں روزے سے ہوں۔",
                    explanation = "روزہ صرف کھانے پینے سے رکنے کا نام نہیں بلکہ زبان اور اخلاق کی حفاظت بھی ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "روزو ڍال آهي",
                    matn = "روزو ڍال آهي، تنهنڪري روزيدار نه فحش ڳالهائي ۽ نه ئي جهالت وارو عمل ڪري، ۽ جيڪڏهن ڪو ساڻس وڙهي يا گار ڏئي ته ٻه ڀيرا چوي ته آءٌ روزي سان آهيان.",
                    explanation = "حقيقي روزو زبان ۽ اخلاق کي به گناهن کان بچائيندو آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_5",
            pillar = HadithPillar.CREED,
            categoryId = "starter_creed",
            categoryTitle = "Pillars of Islam",
            attribution = "Sahih al-Bukhari 8, Sahih Muslim 16",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "بني الإسلام على خمس",
                matn = "بُنِيَ الإِسْلامُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لا إِلَهَ إِلا اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاةِ، وَإِيتَاءِ الزَّكَاةِ، وَالْحَجِّ، وَصَوْمِ رَمَضَانَ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Islam Is Built Upon Five",
                    matn = "Islam is built upon five: testifying that there is no deity worthy of worship except Allah and that Muhammad is the Messenger of Allah, establishing prayer, giving Zakat, performing Hajj, and fasting Ramadan.",
                    explanation = "These five pillars anchor faith in both heart conviction and regular acts of devotion."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "اسلام کی بنیاد پانچ چیزوں پر ہے",
                    matn = "اسلام کی بنیاد پانچ چیزوں پر رکھی گئی ہے: اس بات کی گواہی دینا کہ اللہ کے سوا کوئی معبود نہیں اور محمد ﷺ اللہ کے رسول ہیں، نماز قائم کرنا، زکوٰۃ ادا کرنا، حج کرنا اور رمضان کے روزے رکھنا۔",
                    explanation = "یہ پانچ ارکان ایمان کو دل کے یقین اور عملی عبادت سے جوڑتے ہیں۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "اسلام جو بنياد پنجن شين تي آهي",
                    matn = "اسلام جو بنياد پنجن شين تي رکيو ويو آهي: هن ڳالهه جي شاهدي ڏيڻ ته الله کان سواءِ ڪو معبود ناهي ۽ محمد ﷺ الله جا رسول آهن، نماز قائم ڪرڻ، زڪوات ادا ڪرڻ، حج ڪرڻ ۽ رمضان جا روزا رکڻ.",
                    explanation = "اهي پنج رڪن ايمان کي دل جي يقين ۽ عملي عبادت سان مضبوط ڪن ٿا."
                )
            )
        ),
        HadithEntry(
            id = "starter_6",
            pillar = HadithPillar.VIRTUES,
            categoryId = "starter_virtues",
            categoryTitle = "Allah Looks at Hearts",
            attribution = "Sahih Muslim 2564",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "إن الله لا ينظر إلى صوركم",
                matn = "إِنَّ اللَّهَ لا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ، وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Allah Looks at Your Hearts and Deeds",
                    matn = "Indeed, Allah does not look at your outward appearances or your wealth, rather He looks at your hearts and your deeds.",
                    explanation = "True honor with Allah rests in a sincere heart and righteous action, not status or possessions."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "اللہ تمہارے دلوں اور اعمال کو دیکھتا ہے",
                    matn = "بے شک اللہ تعالیٰ تمہاری صورتوں اور تمہارے مالوں کو نہیں دیکھتا، بلکہ وہ تمہارے دلوں اور تمہارے اعمال کو دیکھتا ہے۔",
                    explanation = "اللہ کے ہاں عزت کا معیار ظاہری شان و شوکت نہیں بلکہ دل کا اخلاص اور نیک عمل ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "الله اوهان جي دلين ۽ عملن کي ڏسي ٿو",
                    matn = "بيشڪ الله تعاليٰ اوهان جي صورتن ۽ مالن کي نٿو ڏسي، بلڪه هو اوهان جي دلين ۽ عملن کي ڏسي ٿو.",
                    explanation = "الله وٽ عزت جو معيار ظاهري ڏيکاءُ نه پر سچي دل ۽ نيڪ عمل آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_7",
            pillar = HadithPillar.VIRTUES,
            categoryId = "starter_virtues",
            categoryTitle = "Relieving Hardship",
            attribution = "Sahih Muslim 2699",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "من نفس عن مؤمن كربة",
                matn = "مَنْ نَفَّسَ عَنْ مُؤْمِنٍ كُرْبَةً مِنْ كُرَبِ الدُّنْيَا نَفَّسَ اللَّهُ عَنْهُ كُرْبَةً مِنْ كُرَبِ يَوْمِ الْقِيَامَةِ، وَمَنْ يَسَّرَ عَلَى مُعْسِرٍ يَسَّرَ اللَّهُ عَلَيْهِ فِي الدُّنْيَا وَالآخِرَةِ، وَاللَّهُ فِي عَوْنِ الْعَبْدِ مَا كَانَ الْعَبْدُ فِي عَوْنِ أَخِيهِ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Whoever Relieves a Believer's Hardship",
                    matn = "Whoever relieves a believer of a distress of this world, Allah will relieve him of a distress on the Day of Resurrection. Whoever makes things easy for one in hardship, Allah will make things easy for him in this world and the Hereafter. Allah is in the aid of His servant so long as the servant is in the aid of his brother.",
                    explanation = "Serving others in difficulty invites Allah's direct help in both worlds."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "مومن کی تکلیف دور کرنے کی فضیلت",
                    matn = "جو شخص کسی مومن سے دنیا کی کوئی تکلیف دور کرے گا اللہ تعالیٰ قیامت کے دن اس کی تکلیفوں میں سے ایک تکلیف دور فرمائے گا، اور جو کسی تنگ دست پر آسانی کرے گا اللہ دنیا و آخرت میں اس پر آسانی فرمائے گا، اور اللہ بندے کی مدد میں رہتا ہے جب تک بندہ اپنے بھائی کی مدد میں رہتا ہے۔",
                    explanation = "دوسروں کے لیے آسانی پیدا کرنا دنیا اور آخرت میں اللہ کی مدد پانے کا ذریعہ ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "مومن جي تڪليف دور ڪرڻ جي فضيلت",
                    matn = "جيڪو شخص ڪنهن مومن تان دنيا جي ڪا تڪليف دور ڪندو، الله تعاليٰ قيامت جي ڏينهن سندس تڪليفن مان هڪ تڪليف دور ڪندو؛ ۽ جيڪو تنگدست تي آساني ڪندو الله دنيا ۽ آخرت ۾ ساڻس آساني ڪندو، ۽ الله ٻانهي جي مدد ۾ رهندو آهي جيستائين ٻانهو پنهنجي ڀاءُ جي مدد ۾ هوندو آهي.",
                    explanation = "انسانن لاءِ آساني پيدا ڪرڻ ٻنهي جهانن ۾ الله جي مدد جو سبب آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_8",
            pillar = HadithPillar.MANNERS,
            categoryId = "starter_manners",
            categoryTitle = "Good Speech & Hospitality",
            attribution = "Sahih al-Bukhari 6018, Sahih Muslim 47",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "من كان يؤمن بالله واليوم الآخر",
                matn = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ، وَمَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيُكْرِمْ جَارَهُ، وَمَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيُكْرِمْ ضَيْفَهُ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Speak Good or Remain Silent",
                    matn = "Whoever believes in Allah and the Last Day, let him speak good or remain silent; whoever believes in Allah and the Last Day, let him honor his neighbor; and whoever believes in Allah and the Last Day, let him honor his guest.",
                    explanation = "Faith expresses itself in disciplined speech, kindness to neighbors, and generous hospitality."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "بھلائی کی بات کہو یا خاموش رہو",
                    matn = "جو شخص اللہ اور آخرت کے دن پر ایمان رکھتا ہو اسے چاہیے کہ اچھی بات کہے یا خاموش رہے، اور جو اللہ اور آخرت کے دن پر ایمان رکھتا ہو وہ اپنے پڑوسی کا اکرام کرے، اور جو اللہ اور آخرت کے دن پر ایمان رکھتا ہو وہ اپنے مہمان کا اکرام کرے۔",
                    explanation = "سچا ایمان زبان کی حفاظت، ہمسائے سے حسنِ سلوک اور مہمان نوازی سے ظاہر ہوتا ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "ڀلائي جي ڳالهه چئو يا خاموش رهو",
                    matn = "جيڪو شخص الله ۽ آخرت جي ڏينهن تي ايمان رکي ٿو تنهن کي گهرجي ته سٺي ڳالهه چوي يا خاموش رهي، جيڪو الله ۽ آخرت جي ڏينهن تي ايمان رکي ٿو سو پنهنجي پاڙيسري جو عزت احترام ڪري، ۽ جيڪو الله ۽ آخرت جي ڏينهن تي ايمان رکي ٿو سو پنهنجي مهمان جي عزت ڪري.",
                    explanation = "ايمان جي علامت زبان جي حفاظت، پاڙيسري سان چڱائي ۽ مهمان نوازي آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_9",
            pillar = HadithPillar.MANNERS,
            categoryId = "starter_manners",
            categoryTitle = "Controlling Anger",
            attribution = "Sahih al-Bukhari 6116",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "لا تغضب",
                matn = "أَنَّ رَجُلًا قَالَ لِلنَّبِيِّ ﷺ: أَوْصِنِي. قَالَ: لا تَغْضَبْ. فَرَدَّدَ مِرَارًا، قَالَ: لا تَغْضَبْ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Do Not Get Angry",
                    matn = "A man said to the Prophet ﷺ: Advise me. He said: Do not get angry. The man repeated his request several times, and each time he ﷺ replied: Do not get angry.",
                    explanation = "Mastering anger protects relationships, prevents regret, and preserves dignity."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "غصہ نہ کرو",
                    matn = "ایک شخص نے نبی کریم ﷺ سے عرض کیا: مجھے کوئی نصیحت فرمائیے۔ آپ ﷺ نے فرمایا: غصہ نہ کیا کرو۔ اس نے کئی بار یہی درخواست دہرائی اور ہر بار آپ ﷺ نے فرمایا: غصہ نہ کیا کرو۔",
                    explanation = "غصے پر قابو پانا تعلقات کو ٹوٹنے سے بچاتا اور انسان کو پشیمانی سے محفوظ رکھتا ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "ڪاوڙ نه ڪر",
                    matn = "هڪ شخص نبي ڪريم ﷺ جن کي عرض ڪيو: مون کي نصيحت فرمايو. پاڻ ﷺ فرمايو: ڪاوڙ نه ڪر. هن ڪيترائي ڀيرا ساڳي درخواست دهرائي، ۽ هر ڀيري پاڻ ﷺ فرمايو: ڪاوڙ نه ڪر.",
                    explanation = "ڪاوڙ تي ضابطو رکڻ رشتن کي بچائي ٿو ۽ انسان کي پڇتاءَ کان محفوظ رکي ٿو."
                )
            )
        ),
        HadithEntry(
            id = "starter_10",
            pillar = HadithPillar.MANNERS,
            categoryId = "starter_manners",
            categoryTitle = "Brotherly Love in Faith",
            attribution = "Sahih al-Bukhari 13, Sahih Muslim 45",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "لا يؤمن أحدكم حتى يحب لأخيه",
                matn = "لا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Love for Your Brother What You Love for Yourself",
                    matn = "None of you truly believes until he loves for his brother what he loves for himself.",
                    explanation = "Complete faith roots out envy and cultivates genuine goodwill for others."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "اپنے بھائی کے لیے وہی پسند کرو جو اپنے لیے کرتے ہو",
                    matn = "تم میں سے کوئی شخص اس وقت تک کامل مومن نہیں ہو سکتا جب تک وہ اپنے بھائی کے لیے وہی پسند نہ کرے جو اپنے لیے پسند کرتا ہے۔",
                    explanation = "ایمان کا کمال حسد سے پاک دل اور دوسروں کی بھلائی چاہنے میں ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "پنهنجي ڀاءُ لاءِ اهو پسند ڪر جيڪو پنهنجي لاءِ ڪرين ٿو",
                    matn = "اوهان مان ڪو به تيسيتائين مڪمل مومن نٿو ٿي سگهي جيستائين هو پنهنجي ڀاءُ لاءِ به اهو ئي پسند نه ڪري جيڪو پنهنجي لاءِ پسند ڪري ٿو.",
                    explanation = "مڪمل ايمان دل مان حسد ختم ڪري ٻين لاءِ ڀلائي چاهڻ سيکاري ٿو."
                )
            )
        ),
        HadithEntry(
            id = "starter_11",
            pillar = HadithPillar.TAFSIR,
            categoryId = "starter_tafsir",
            categoryTitle = "Learning & Teaching the Qur'an",
            attribution = "Sahih al-Bukhari 5027",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "خيركم من تعلم القرآن وعلمه",
                matn = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "The Best Among You Learn and Teach the Qur'an",
                    matn = "The best among you are those who learn the Qur'an and teach it.",
                    explanation = "Engaging with the Book of Allah—reciting, understanding its meanings, and passing it on—elevates a person in honor."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "تم میں سب سے بہتر قرآن سیکھنے اور سکھانے والا ہے",
                    matn = "تم میں سے سب سے بہتر وہ ہے جو قرآن سیکھے اور سکھائے۔",
                    explanation = "قرآن مجید کی تلاوت، اس کے معانی سمجھنے اور دوسروں تک پہنچانے میں سب سے بڑی فضیلت ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "اوهان ۾ سڀ کان بهتر قرآن سکندڙ ۽ سيکاريندڙ آهي",
                    matn = "اوهان مان سڀ کان بهتر اهو آهي جيڪو قرآن سکي ۽ سيکاري.",
                    explanation = "قرآن مجيد پڙهڻ، ان جو مفهوم سمجهڻ ۽ ٻين کي سيکارڻ سڀ کان وڏي فضيلت آهي."
                )
            )
        ),
        HadithEntry(
            id = "starter_12",
            pillar = HadithPillar.TAFSIR,
            categoryId = "starter_tafsir",
            categoryTitle = "Virtue of Surah Al-Ikhlas",
            attribution = "Sahih al-Bukhari 5015, Sahih Muslim 811",
            grade = "Sahih",
            arabic = HadithTextVariant(
                title = "قل هو الله أحد تعدل ثلث القرآن",
                matn = "أَيَعْجِزُ أَحَدُكُمْ أَنْ يَقْرَأَ فِي لَيْلَةٍ ثُلُثَ الْقُرْآنِ؟ قَالُوا: وَكَيْفَ يَقْرَأُ ثُلُثَ الْقُرْآنِ؟ قَالَ: قُلْ هُوَ اللَّهُ أَحَدٌ تَعْدِلُ ثُلُثَ الْقُرْآنِ."
            ),
            translations = mapOf(
                HadithLanguage.ENGLISH to HadithTextVariant(
                    title = "Surah Al-Ikhlas Equals One-Third of the Qur'an",
                    matn = "Is any one of you incapable of reciting one-third of the Qur'an in a single night? They asked: How can one recite one-third of the Qur'an? He ﷺ said: 'Qul Huwa Allahu Ahad' (Surah Al-Ikhlas) is equivalent to one-third of the Qur'an.",
                    explanation = "Because Surah Al-Ikhlas purely affirms Allah's Oneness (Tawhid), it carries one-third of the Qur'an's thematic core."
                ),
                HadithLanguage.URDU to HadithTextVariant(
                    title = "سورۃ الاخلاص تہائی قرآن کے برابر ہے",
                    matn = "کیا تم میں سے کوئی اس بات سے عاجز ہے کہ ایک رات میں تہائی قرآن پڑھ لے؟ صحابہ نے عرض کیا: ایک رات میں تہائی قرآن کیسے پڑھا جا سکتا ہے؟ آپ ﷺ نے فرمایا: ’قُلْ هُوَ اللَّهُ أَحَدٌ‘ تہائی قرآن کے برابر ہے۔",
                    explanation = "سورۃ الاخلاص خالص توحیدِ الٰہی کا بیان ہے، اسی لیے مضمون کے لحاظ سے تہائی قرآن کے مساوی ہے۔"
                ),
                HadithLanguage.SINDHI to HadithTextVariant(
                    title = "سورة الاخلاص ٽئين حصي قرآن جي برابر آهي",
                    matn = "ڇا اوهان مان ڪو هن ڳالهه کان عاجز آهي جو هڪ رات ۾ قرآن جو ٽيون حصو پڙهي؟ صحابن عرض ڪيو: هڪ رات ۾ ٽيون حصو قرآن ڪيئن پڙهي سگهجي ٿو؟ پاڻ ﷺ فرمايو: ’قُلْ هُوَ اللَّهُ أَحَدٌ‘ قرآن جي ٽئين حصي جي برابر آهي.",
                    explanation = "سورة الاخلاص خالص توحيد جو بيان آهي، ان ڪري معنيٰ جي لحاظ کان ٽئين حصي قرآن جي برابر آهي."
                )
            )
        )
    )
}
