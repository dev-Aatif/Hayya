package dev.hayya.core.domain.hadith

/**
 * Condenses lengthy classical narration chains (*Isnad*) in Urdu, Sindhi, and Arabic
 * canonical Hadith collections down to the key 2-3 recognizable narrators (typically the
 * Companion plus 1-2 immediate/well-known predecessors) while keeping short chains,
 * English translations, and non-chain prose untouched.
 *
 * Formatting is non-destructive and applied at display/share time so full-text search
 * and on-demand full-chain inspection retain every narrator in SQLite.
 */
object HadithIsnadFormatter {

    private const val MAX_PREFIX_SCAN_CHARS = 900
    private const val MIN_LONG_TEXT_LENGTH = 130

    private val ZWNJ_OR_TATWEEL = setOf('\u200C', '\u200D', '\u0640')

    private val URDU_SINDHI_HONORIFIC_REGEX = Regex(
        """رض[یي]\s+الل[هہ]\s+عن(?:ہما|هما|ہم|هم|ہا|ها|ہ|ه)"""
    )

    private val URDU_MATN_BREAK_REGEX = Regex(
        """(?:فرمایا|فرماتے|ارشاد|چيو|فرمايو|سوال\s+کیا|رسول\s+الل[ہه]|نبی\s+کریم|نبی\s+اکرم|آنحضرت|نبي\s+کريم|پاڻ\s+سڳورن|”|"|«|:)"""
    )

    private val CHAIN_VERB_REGEX = Regex(
        """(?:بیان\s+کیا|بیان\s+کی|خبر\s+دی|روایت\s+کی|روایت\s+ہے|نقل\s+کی|نقل\s+کیا|حاصل\s+ہوئی|سنا|بيان\s+ڪيو|خبر\s+ڏني|روايت\s+آهي|ٻڌو|سے\s*$|نے\s*$)"""
    )

    private val LEADING_CHAIN_FILLER_REGEX = Regex(
        """^(?:(?:اور|پھر|کہا|ان\s+کا\s+بیان\s+ہے\s+کہ|وہ\s+کہتے\s+(?:ہیں|تھے)(?:\s+کہ)?|انہوں\s+نے\s+(?:کہا\s+کہ|اس\s+حدیث\s+کو)?|ہم\s+(?:سے|کو)|ان\s+(?:کو|سے|کے|کی)|کو|مجھے|اسے|انہیں|کہ|یہ\s+حدیث|اس\s+کی\s+بابت|۽|چيو\s+ته|هنن\s+چيو\s+ته|اسان\s+کي|مون\s+کي)\s*)+"""
    )

    private val TRAILING_CHAIN_VERB_REGEX = Regex(
        """\s*(?:نے|سے|کو|کان|کي)?\s*(?:(?:یہ\s+)?حدیث\s+)?(?:کی\s+روایت\s+سے.*|(?:بیان\s+کیا|بیان\s+کی|خبر\s+دی|روایت\s+کی|روایت\s+ہے|نقل\s+کی|نقل\s+کیا|حاصل\s+ہوئی|سنا|بيان\s+ڪيو|خبر\s+ڏني|روايت\s+آهي|ٻڌو).*)$"""
    )

    private val GENERIC_NARRATOR_REGEX = Regex(
        """^(?:اپنے\s+والد|اپنی\s+والدہ|اپنے\s+باپ|اپنے\s+بھائی|اپنے\s+چچا|ان\s+کے\s+والد|سندس\s+والد|انهن|انہوں|ان|کو|اس)$"""
    )

    private val SAHABI_PREFIX_STRIP_REGEX = Regex(
        """^(?:(?:اور|پھر|کہ|انہوں\s+نے|ان\s+سے|ان\s+کو|هنن|انهن(?:\s+کان)?|ان\s+کا\s+بیان\s+ہے\s+کہ|وہ\s+کہتے\s+(?:ہیں|تھے)\s+کہ|میں\s+نے\s+مسجد\s+نبوی\s+میں\s+منبر\s+رسول\s+صلی\s+اللہ\s+علیہ\s+وسلم\s+پر|منبر\s+رسول\s+صلی\s+اللہ\s+علیہ\s+وسلم\s+پر)\s*)+"""
    )

    private val POST_HONORIFIC_BRIDGE_REGEX = Regex(
        """^\s*(?:کی\s+زبان\s+سے\s+سنا|سے\s+(?:نقل\s+کی|نقل\s+کیا|روایت\s+کی|روایت\s+ہے|بیان\s+کیا|سنا|مروی\s+ہے)|نے\s+(?:روایت\s+کی|بیان\s+کیا|سنا)|کان\s+روايت\s+(?:آهي|ڪيو|ڪئي)|کان|نے|سے)?(?:\s*[،,]?\s*(?:وہ\s+(?:فرما|کہتے)\s+(?:رہے\s+)?تھے\s+کہ|جو\s+فرما\s+رہے\s+تھے\s+کہ|کہ|ته))?\s*"""
    )

    private val KNOWN_NARRATOR_KEYWORDS = listOf(
        "امام مالک", "مالک", "سفیان", "زہری", "نافع", "علقمہ", "ہشام بن عروہ",
        "عروہ", "عکرمہ", "سعید بن مسیب", "ابوسلمہ", "ابوزرعہ", "قتادہ", "شعبہ",
        "اعمش", "عطاء", "حسن بصری", "مجاہد", "یحییٰ بن سعید", "محمد بن سیرین",
        "ثابت", "حماد", "لیث", "اوزاعی", "ابن جریج", "معمر"
    )

    private val ARABIC_ISNAD_START_REGEX = Regex(
        """^(?:حدثنا|حدثني|أخبرنا|أخبرني|اخبرنا|اخبرني)\s+"""
    )

    private val ARABIC_MATN_ANCHOR_REGEX = Regex(
        """(?:رسول\s+الله|النبي|نبي\s+الله)"""
    )

    private val ARABIC_AN_LINK_REGEX = Regex(
        """(?:^|[،,\s])(?:عن|وحدثني|وحدثنا)\s+"""
    )

    /**
     * Formats translation [text] for concise reading in the given [language].
     */
    fun formatTranslation(text: String, language: HadithLanguage): String {
        if (text.isBlank()) return text
        return when (language) {
            HadithLanguage.ENGLISH -> text
            HadithLanguage.URDU, HadithLanguage.SINDHI ->
                condenseUrduOrSindhi(text, isSindhi = language == HadithLanguage.SINDHI)
        }
    }

    /**
     * Formats classical Arabic [text] by trimming chains of 4+ narrators to the last 2-3 narrators.
     */
    fun formatArabic(text: String): String {
        if (text.length < MIN_LONG_TEXT_LENGTH) return text
        val mapped = normalizeWithMap(text, MAX_PREFIX_SCAN_CHARS, stripArabicHarakat = true)
        if (!ARABIC_ISNAD_START_REGEX.containsMatchIn(mapped.clean)) return text

        val anchorMatch = ARABIC_MATN_ANCHOR_REGEX.find(mapped.clean) ?: return text
        val prefixClean = mapped.clean.substring(0, anchorMatch.range.first)

        val linkStarts = ARABIC_AN_LINK_REGEX.findAll(prefixClean)
            .map { match ->
                val raw = match.value
                val trimmedStart = raw.indexOfFirst { ch -> ch != ' ' && ch != '،' && ch != ',' }
                if (trimmedStart >= 0) match.range.first + trimmedStart else match.range.first
            }
            .toList()

        // Only shorten when the chain has >= 4 links (`حدثنا...` plus 3+ `عن` links)
        if (linkStarts.size < 3) return text
        val targetCleanIdx = linkStarts[linkStarts.size - 2]
        val origStart = mapped.origIndex(targetCleanIdx) ?: return text
        if (origStart <= 0 || origStart >= text.length) return text
        return text.substring(origStart).trimStart(' ', '،', ',', ':')
    }

    /**
     * Returns true when formatting [text] for [language] shortens a long narration chain.
     */
    fun isCondensed(text: String, language: HadithLanguage): Boolean {
        return formatTranslation(text, language) != text
    }

    /**
     * Returns true when formatting Arabic [text] shortens a long narration chain.
     */
    fun isArabicCondensed(text: String): Boolean {
        return formatArabic(text) != text
    }

    private fun condenseUrduOrSindhi(text: String, isSindhi: Boolean): String {
        if (text.length < MIN_LONG_TEXT_LENGTH) return text
        val mapped = normalizeWithMap(text, MAX_PREFIX_SCAN_CHARS, stripArabicHarakat = false)

        val honorificMatches = URDU_SINDHI_HONORIFIC_REGEX.findAll(mapped.clean).toList()
        if (honorificMatches.isEmpty()) return text

        // Pick the final Companion honorific in the opening chain before Prophetic speech starts
        var chosenHonorific = honorificMatches.first()
        for (i in 1 until honorificMatches.size) {
            val candidate = honorificMatches[i]
            val gapStart = chosenHonorific.range.last + 1
            val gapEnd = candidate.range.first
            if (gapEnd - gapStart > 90) break
            val gap = mapped.clean.substring(gapStart, gapEnd)
            if (URDU_MATN_BREAK_REGEX.containsMatchIn(gap)) break
            chosenHonorific = candidate
        }

        val clauseStartClean = findClauseStart(mapped.clean, chosenHonorific.range.first)
        val prefixClean = mapped.clean.substring(0, clauseStartClean)
        val rawClauses = prefixClean
            .split(Regex("""[،۔,.]+|\s+(?:اور|۽)\s+(?=(?:ان\s|انہوں|انہیں|وہ|هنن|انهن))"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val isLongChain = rawClauses.size >= 3 || (rawClauses.size == 2 && clauseStartClean >= 130)
        if (!isLongChain) return text
        if (rawClauses.none { CHAIN_VERB_REGEX.containsMatchIn(it) }) return text

        val extractedNames = rawClauses.flatMap { extractNarratorNamesFromClause(it) }
            .distinct()
        val selectedPredecessors = selectKeyNarrators(extractedNames, maxCount = 2)

        val sahabiClauseClean = mapped.clean
            .substring(clauseStartClean, chosenHonorific.range.last + 1)
            .trim()
        val sahabiDisplay = cleanSahabiClause(sahabiClauseClean)
        if (sahabiDisplay.isBlank()) return text

        val postHonorificOrigStart = mapped.origEndIndex(chosenHonorific.range.last) ?: return text
        if (postHonorificOrigStart >= text.length) return text

        val tailClean = mapped.clean.substring(chosenHonorific.range.last + 1)
        val bridgeMatch = POST_HONORIFIC_BRIDGE_REGEX.find(tailClean)
        val bodyCleanStart = chosenHonorific.range.last + 1 + (bridgeMatch?.value?.length ?: 0)
        val bodyOrigStart = if (bodyCleanStart < mapped.clean.length) {
            mapped.origIndex(bodyCleanStart) ?: postHonorificOrigStart
        } else {
            return text
        }

        val matnBody = text.substring(bodyOrigStart).trimStart(' ', '،', ',', '۔', ':', '؛', '\u200C')
        if (matnBody.length < 25) return text

        val prefixNarrators = if (selectedPredecessors.isNotEmpty()) {
            selectedPredecessors.joinToString("، ") + " — "
        } else {
            ""
        }
        val narrationVerb = if (isSindhi) "کان روايت آهي:" else "سے روایت ہے:"
        return "$prefixNarrators$sahabiDisplay $narrationVerb $matnBody"
    }

    private fun findClauseStart(clean: String, pos: Int): Int {
        var idx = pos - 1
        while (idx >= 0) {
            val ch = clean[idx]
            if (ch == '،' || ch == '۔' || ch == ',' || ch == '.') {
                var start = idx + 1
                while (start < pos && clean[start].isWhitespace()) {
                    start++
                }
                return start
            }
            if ((ch == '۽' || (idx >= 2 && clean[idx] == 'ر' && clean[idx - 1] == 'و' && clean[idx - 2] == 'ا' && (idx == 2 || clean[idx - 3].isWhitespace()))) &&
                idx + 1 < pos && clean[idx + 1].isWhitespace()
            ) {
                val afterConnector = clean.substring(idx + 1, pos).trimStart()
                if (afterConnector.startsWith("هنن ") ||
                    afterConnector.startsWith("انهن ") ||
                    afterConnector.startsWith("ان ") ||
                    afterConnector.startsWith("انہوں ")
                ) {
                    var start = idx + 1
                    while (start < pos && clean[start].isWhitespace()) {
                        start++
                    }
                    return start
                }
            }
            idx--
        }
        return 0
    }

    private fun extractNarratorNamesFromClause(clause: String): List<String> {
        var stripped = clause.trim()
        // Repeatedly trim leading fillers (`انہوں نے کہا کہ`, `ہم سے`, etc.)
        var prev: String
        do {
            prev = stripped
            stripped = LEADING_CHAIN_FILLER_REGEX.replace(stripped, "").trim()
        } while (stripped != prev)

        stripped = TRAILING_CHAIN_VERB_REGEX.replace(stripped, "").trim()
        if (stripped.isEmpty()) return emptyList()

        // Clauses like `ابوحیان تیمی نے ابوزرعہ سے` or `مالک نے ہشام بن عروہ`
        val parts = stripped.split(Regex("""\s+(?:نے|اور)\s+|\s+سے\s+"""))
            .map { cleanSingleNarratorToken(it) }
            .filter { it.isNotBlank() && !GENERIC_NARRATOR_REGEX.matches(it) }

        return parts.ifEmpty {
            listOf(cleanSingleNarratorToken(stripped))
                .filter { it.isNotBlank() && !GENERIC_NARRATOR_REGEX.matches(it) }
        }
    }

    private fun cleanSingleNarratorToken(raw: String): String {
        var s = raw.trim(' ', '،', ',', '۔', '.', '(', ')')
        s = LEADING_CHAIN_FILLER_REGEX.replace(s, "").trim()
        s = URDU_SINDHI_HONORIFIC_REGEX.replace(s, "").trim()
        if (s.length < 3 || s.length > 48) return ""
        return s
    }

    private fun selectKeyNarrators(names: List<String>, maxCount: Int): List<String> {
        if (names.isEmpty() || maxCount <= 0) return emptyList()
        if (names.size <= maxCount) return names

        val pickedIndices = LinkedHashSet<Int>()
        // Prefer an earlier well-known Imam / Tabi'i first, then the immediate predecessor to the Sahabi
        val knownIdx = names.indexOfFirst { name ->
            KNOWN_NARRATOR_KEYWORDS.any { keyword -> name.contains(keyword) }
        }
        if (knownIdx >= 0) {
            pickedIndices.add(knownIdx)
        }
        pickedIndices.add(names.lastIndex)

        var backIdx = names.lastIndex - 1
        while (pickedIndices.size < maxCount && backIdx >= 0) {
            pickedIndices.add(backIdx)
            backIdx--
        }

        return pickedIndices.sorted().take(maxCount).map { names[it] }
    }

    private fun cleanSahabiClause(clause: String): String {
        var s = clause.trim()
        var prev: String
        do {
            prev = s
            s = SAHABI_PREFIX_STRIP_REGEX.replace(s, "").trim()
        } while (s != prev)
        return s.trim(' ', '،', ',', '۔', '.')
    }

    private class MappedText(
        val clean: String,
        private val origIndices: IntArray
    ) {
        fun origIndex(cleanIdx: Int): Int? =
            origIndices.getOrNull(cleanIdx)

        fun origEndIndex(cleanLastIdx: Int): Int? =
            origIndices.getOrNull(cleanLastIdx)?.plus(1)
    }

    private fun normalizeWithMap(
        raw: String,
        maxOrigChars: Int,
        stripArabicHarakat: Boolean
    ): MappedText {
        val limit = raw.length.coerceAtMost(maxOrigChars)
        val sb = StringBuilder(limit)
        val map = IntArray(limit)
        var cleanLen = 0
        var lastWasSpace = true

        for (i in 0 until limit) {
            val ch = raw[i]
            if (ch in ZWNJ_OR_TATWEEL) continue
            if (stripArabicHarakat && isArabicDiacritic(ch)) continue

            if (ch.isWhitespace() || ch == '\u00A0') {
                if (!lastWasSpace) {
                    sb.append(' ')
                    map[cleanLen++] = i
                    lastWasSpace = true
                }
            } else {
                sb.append(ch)
                map[cleanLen++] = i
                lastWasSpace = false
            }
        }
        return MappedText(sb.toString(), map.copyOf(cleanLen))
    }

    private fun isArabicDiacritic(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x064B..0x065F) || code == 0x0670
    }
}
