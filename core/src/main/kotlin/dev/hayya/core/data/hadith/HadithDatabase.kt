package dev.hayya.core.data.hadith

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.hayya.core.domain.hadith.CanonicalHadithBooks
import dev.hayya.core.domain.hadith.HadithBookInfo
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.domain.hadith.HadithTextVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local SQLite + FTS4 database storing bundled starter Hadiths, synced HadeethEnc
 * thematic entries, and full canonical books from fawazahmed0/hadith-api.
 */
class HadithDatabase(
    context: Context,
    dbName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    private val _changes = MutableStateFlow(0L)
    val changes: StateFlow<Long> = _changes.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hadiths (
                id TEXT PRIMARY KEY,
                pillar TEXT NOT NULL,
                category_id TEXT NOT NULL,
                category_title TEXT NOT NULL,
                attribution TEXT NOT NULL,
                grade TEXT NOT NULL,
                arabic_title TEXT NOT NULL,
                arabic_matn TEXT NOT NULL,
                arabic_explanation TEXT NOT NULL DEFAULT '',
                book_id TEXT NOT NULL DEFAULT '',
                book_name TEXT NOT NULL DEFAULT '',
                is_priority INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hadith_translations (
                hadith_id TEXT NOT NULL,
                lang TEXT NOT NULL,
                title TEXT NOT NULL,
                matn TEXT NOT NULL,
                explanation TEXT NOT NULL DEFAULT '',
                hints TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (hadith_id, lang)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hadith_user_state (
                hadith_id TEXT PRIMARY KEY,
                is_bookmarked INTEGER NOT NULL DEFAULT 0,
                is_read INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hadith_categories (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                pillar TEXT NOT NULL,
                remote_count INTEGER NOT NULL DEFAULT 0,
                synced_count INTEGER NOT NULL DEFAULT 0,
                synced_pages INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hadith_books (
                book_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                downloaded_count INTEGER NOT NULL DEFAULT 0,
                downloaded_langs TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS hadith_fts USING fts4(
                hadith_id,
                title,
                matn,
                attribution
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS hadith_fts_map (
                hadith_id TEXT PRIMARY KEY,
                fts_rowid INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_hadiths_pillar ON hadiths(pillar)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_hadiths_category ON hadiths(category_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_hadiths_book ON hadiths(book_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE hadiths ADD COLUMN book_id TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE hadiths ADD COLUMN book_name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE hadiths ADD COLUMN is_priority INTEGER NOT NULL DEFAULT 1")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS hadith_books (
                    book_id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    downloaded_count INTEGER NOT NULL DEFAULT 0,
                    downloaded_langs TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_hadiths_book ON hadiths(book_id)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE hadith_categories ADD COLUMN synced_pages INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS hadith_fts_map (
                    hadith_id TEXT PRIMARY KEY,
                    fts_rowid INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO hadith_fts_map(hadith_id, fts_rowid)
                SELECT hadith_id, docid FROM hadith_fts
                """.trimIndent()
            )
        }
    }

    suspend fun ensureStarterSeeded() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val existingCount = db.rawQuery("SELECT COUNT(*) FROM hadiths", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        if (existingCount == 0) {
            upsertHadithsInternal(db, HadithStarterPack.entries)
            notifyChanged()
        }
    }

    suspend fun upsertHadiths(entries: List<HadithEntry>) = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext
        upsertHadithsInternal(writableDatabase, entries)
        notifyChanged()
    }

    suspend fun saveDownloadedBook(
        bookId: String,
        bookTitle: String,
        downloadedLanguages: List<String>,
        entries: List<HadithEntry>
    ) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            upsertHadithsInternal(db, entries)
            val values = ContentValues().apply {
                put("book_id", bookId)
                put("title", bookTitle)
                put("downloaded_count", entries.size)
                put("downloaded_langs", downloadedLanguages.joinToString(","))
            }
            db.insertWithOnConflict("hadith_books", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                "DELETE FROM hadith_translations WHERE hadith_id IN (SELECT id FROM hadiths WHERE book_id = ?)",
                arrayOf(bookId)
            )
            db.execSQL(
                "DELETE FROM hadith_fts WHERE docid IN (SELECT m.fts_rowid FROM hadith_fts_map m INNER JOIN hadiths h ON h.id = m.hadith_id WHERE h.book_id = ?)",
                arrayOf(bookId)
            )
            db.execSQL(
                "DELETE FROM hadith_fts_map WHERE hadith_id IN (SELECT id FROM hadiths WHERE book_id = ?)",
                arrayOf(bookId)
            )
            db.delete("hadiths", "book_id = ?", arrayOf(bookId))
            db.delete("hadith_books", "book_id = ?", arrayOf(bookId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    suspend fun getBooksCatalog(): List<HadithBookInfo> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val stored = HashMap<String, Pair<Int, List<String>>>()
        db.rawQuery(
            "SELECT book_id, downloaded_count, downloaded_langs FROM hadith_books",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val count = cursor.getInt(1)
                val langs = cursor.getString(2).split(',').map { it.trim() }.filter { it.isNotEmpty() }
                stored[id] = Pair(count, langs)
            }
        }
        CanonicalHadithBooks.catalog.map { book ->
            val info = stored[book.bookId]
            if (info != null) {
                book.copy(
                    downloadedCount = info.first,
                    downloadedLanguages = info.second
                )
            } else {
                book
            }
        }
    }

    suspend fun upsertTranslationVariant(
        hadithId: String,
        language: HadithLanguage,
        variant: HadithTextVariant
    ) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("hadith_id", hadithId)
                put("lang", language.code)
                put("title", variant.title)
                put("matn", variant.matn)
                put("explanation", variant.explanation)
                put("hints", variant.hints.joinToString("\n"))
            }
            db.insertWithOnConflict("hadith_translations", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            rebuildFtsRowFromDb(db, hadithId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    suspend fun queryHadiths(
        pillar: HadithPillar? = null,
        onlyBookmarked: Boolean = false,
        searchQuery: String = "",
        bookId: String? = null,
        limit: Int = DEFAULT_QUERY_LIMIT
    ): List<HadithEntry> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val trimmedQuery = searchQuery.trim()
        val matchingIds = if (trimmedQuery.isNotEmpty()) {
            findMatchingIds(db, trimmedQuery, limit)
        } else {
            null
        }
        if (matchingIds != null && matchingIds.isEmpty()) {
            return@withContext emptyList()
        }

        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!bookId.isNullOrBlank()) {
            whereClauses.add("h.book_id = ?")
            args.add(bookId)
        } else if (!onlyBookmarked && trimmedQuery.isEmpty()) {
            // Keep curated thematic feed focused on Worship/Creed/Virtues/Manners/Tafsir
            whereClauses.add("h.is_priority = 1")
        }

        if (pillar != null) {
            whereClauses.add("h.pillar = ?")
            args.add(pillar.id)
        }
        if (onlyBookmarked) {
            whereClauses.add("COALESCE(u.is_bookmarked, 0) = 1")
        }
        if (matchingIds != null) {
            val placeholders = matchingIds.joinToString(",") { "?" }
            whereClauses.add("h.id IN ($placeholders)")
            args.addAll(matchingIds)
        }

        val whereSql = if (whereClauses.isEmpty()) "" else "WHERE ${whereClauses.joinToString(" AND ")}"
        val orderSql = if (!bookId.isNullOrBlank()) {
            "ORDER BY CAST(REPLACE(h.id, 'fw_' || h.book_id || '_', '') AS REAL) ASC, h.id ASC"
        } else {
            "ORDER BY CASE WHEN h.id LIKE 'starter_%' THEN 1 ELSE 0 END ASC, CASE WHEN h.book_id = '' THEN 0 ELSE 1 END ASC, h.updated_at DESC, h.id ASC"
        }

        val sql = """
            SELECT h.id, h.pillar, h.category_id, h.category_title, h.attribution, h.grade,
                   h.arabic_title, h.arabic_matn, h.arabic_explanation,
                   COALESCE(u.is_bookmarked, 0) AS is_bookmarked,
                   COALESCE(u.is_read, 0) AS is_read,
                   h.book_id, h.book_name, h.is_priority
            FROM hadiths h
            LEFT JOIN hadith_user_state u ON u.hadith_id = h.id
            $whereSql
            $orderSql
            LIMIT $limit
        """.trimIndent()

        val baseRows = mutableListOf<HadithEntry>()
        db.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                baseRows.add(
                    HadithEntry(
                        id = cursor.getString(0),
                        pillar = HadithPillar.fromId(cursor.getString(1)),
                        categoryId = cursor.getString(2),
                        categoryTitle = cursor.getString(3),
                        attribution = cursor.getString(4),
                        grade = cursor.getString(5),
                        arabic = HadithTextVariant(
                            title = cursor.getString(6),
                            matn = cursor.getString(7),
                            explanation = cursor.getString(8)
                        ),
                        translations = emptyMap(),
                        isBookmarked = cursor.getInt(9) == 1,
                        isRead = cursor.getInt(10) == 1,
                        bookId = cursor.getString(11),
                        bookName = cursor.getString(12),
                        isPriority = cursor.getInt(13) == 1
                    )
                )
            }
        }

        if (baseRows.isEmpty()) return@withContext emptyList()
        val translationsById = loadTranslationsMap(db, baseRows.map { it.id })
        baseRows.map { row ->
            row.copy(translations = translationsById[row.id].orEmpty())
        }
    }

    suspend fun getPriorityPoolForDailySelection(limit: Int = 200): List<HadithEntry> =
        queryHadiths(pillar = null, onlyBookmarked = false, searchQuery = "", bookId = null, limit = limit)

    suspend fun getHadithById(hadithId: String): HadithEntry? = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val sql = """
            SELECT h.id, h.pillar, h.category_id, h.category_title, h.attribution, h.grade,
                   h.arabic_title, h.arabic_matn, h.arabic_explanation,
                   COALESCE(u.is_bookmarked, 0) AS is_bookmarked,
                   COALESCE(u.is_read, 0) AS is_read,
                   h.book_id, h.book_name, h.is_priority
            FROM hadiths h
            LEFT JOIN hadith_user_state u ON u.hadith_id = h.id
            WHERE h.id = ?
            LIMIT 1
        """.trimIndent()

        val entry = db.rawQuery(sql, arrayOf(hadithId)).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            HadithEntry(
                id = cursor.getString(0),
                pillar = HadithPillar.fromId(cursor.getString(1)),
                categoryId = cursor.getString(2),
                categoryTitle = cursor.getString(3),
                attribution = cursor.getString(4),
                grade = cursor.getString(5),
                arabic = HadithTextVariant(
                    title = cursor.getString(6),
                    matn = cursor.getString(7),
                    explanation = cursor.getString(8)
                ),
                translations = emptyMap(),
                isBookmarked = cursor.getInt(9) == 1,
                isRead = cursor.getInt(10) == 1,
                bookId = cursor.getString(11),
                bookName = cursor.getString(12),
                isPriority = cursor.getInt(13) == 1
            )
        } ?: return@withContext null

        val translations = loadTranslationsMap(db, listOf(hadithId))[hadithId].orEmpty()
        entry.copy(translations = translations)
    }

    suspend fun toggleBookmark(hadithId: String): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val current = getUserState(db, hadithId)
        val nextBookmark = !current.first
        saveUserState(db, hadithId, nextBookmark, current.second)
        notifyChanged()
        nextBookmark
    }

    suspend fun setRead(hadithId: String, isRead: Boolean) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val current = getUserState(db, hadithId)
        saveUserState(db, hadithId, current.first, isRead)
        notifyChanged()
    }

    suspend fun upsertCategories(categories: List<HadithCategorySyncInfo>) = withContext(Dispatchers.IO) {
        if (categories.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            categories.forEach { cat ->
                val syncedCount = db.rawQuery(
                    "SELECT COUNT(*) FROM hadiths WHERE category_id = ?",
                    arrayOf(cat.id)
                ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

                val existingPages = db.rawQuery(
                    "SELECT synced_pages FROM hadith_categories WHERE id = ?",
                    arrayOf(cat.id)
                ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

                val values = ContentValues().apply {
                    put("id", cat.id)
                    put("title", cat.title)
                    put("pillar", cat.pillar.id)
                    put("remote_count", cat.remoteCount)
                    put("synced_count", syncedCount)
                    put("synced_pages", maxOf(existingPages, cat.syncedPages))
                }
                db.insertWithOnConflict("hadith_categories", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    suspend fun markCategoryPageSynced(categoryId: String, page: Int) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.execSQL(
            """
            UPDATE hadith_categories
            SET synced_pages = CASE WHEN ? > synced_pages THEN ? ELSE synced_pages END,
                synced_count = (SELECT COUNT(*) FROM hadiths h WHERE h.category_id = ?)
            WHERE id = ?
            """.trimIndent(),
            arrayOf(page, page, categoryId, categoryId)
        )
        notifyChanged()
    }

    suspend fun getCategories(): List<HadithCategorySyncInfo> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val sql = """
            SELECT c.id, c.title, c.pillar, c.remote_count,
                   (SELECT COUNT(*) FROM hadiths h WHERE h.category_id = c.id) AS synced_count,
                   c.synced_pages
            FROM hadith_categories c
            ORDER BY c.pillar ASC, c.title ASC
        """.trimIndent()
        val list = mutableListOf<HadithCategorySyncInfo>()
        db.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    HadithCategorySyncInfo(
                        id = cursor.getString(0),
                        title = cursor.getString(1),
                        pillar = HadithPillar.fromId(cursor.getString(2)),
                        remoteCount = cursor.getInt(3),
                        syncedCount = cursor.getInt(4),
                        syncedPages = cursor.getInt(5)
                    )
                )
            }
        }
        list
    }

    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM hadiths", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun upsertHadithsInternal(db: SQLiteDatabase, entries: List<HadithEntry>) {
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            entries.forEach { entry ->
                val hadithValues = ContentValues().apply {
                    put("id", entry.id)
                    put("pillar", entry.pillar.id)
                    put("category_id", entry.categoryId)
                    put("category_title", entry.categoryTitle)
                    put("attribution", entry.attribution)
                    put("grade", entry.grade)
                    put("arabic_title", entry.arabic.title)
                    put("arabic_matn", entry.arabic.matn)
                    put("arabic_explanation", entry.arabic.explanation)
                    put("book_id", entry.bookId)
                    put("book_name", entry.bookName)
                    put("is_priority", if (entry.isPriority) 1 else 0)
                    put("updated_at", now)
                }
                db.insertWithOnConflict("hadiths", null, hadithValues, SQLiteDatabase.CONFLICT_REPLACE)

                entry.translations.forEach { (lang, variant) ->
                    val trValues = ContentValues().apply {
                        put("hadith_id", entry.id)
                        put("lang", lang.code)
                        put("title", variant.title)
                        put("matn", variant.matn)
                        put("explanation", variant.explanation)
                        put("hints", variant.hints.joinToString("\n"))
                    }
                    db.insertWithOnConflict("hadith_translations", null, trValues, SQLiteDatabase.CONFLICT_REPLACE)
                }
                writeFtsRowInMemory(db, entry)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun writeFtsRowInMemory(db: SQLiteDatabase, entry: HadithEntry) {
        deleteFtsRowById(db, entry.id)

        val titles = StringBuilder(entry.arabic.title)
        val matns = StringBuilder(entry.arabic.matn)
        entry.translations.values.forEach { variant ->
            if (variant.title.isNotBlank()) titles.append(' ').append(variant.title)
            if (variant.matn.isNotBlank()) matns.append(' ').append(variant.matn)
        }

        val ftsValues = ContentValues().apply {
            put("hadith_id", entry.id)
            put("title", titles.toString())
            put("matn", matns.toString())
            put("attribution", entry.attribution)
        }
        val rowId = db.insert("hadith_fts", null, ftsValues)
        if (rowId >= 0L) {
            val mapValues = ContentValues().apply {
                put("hadith_id", entry.id)
                put("fts_rowid", rowId)
            }
            db.insertWithOnConflict("hadith_fts_map", null, mapValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun rebuildFtsRowFromDb(db: SQLiteDatabase, hadithId: String) {
        deleteFtsRowById(db, hadithId)

        var titleAgg = ""
        var matnAgg = ""
        var attribution = ""

        db.rawQuery(
            "SELECT arabic_title, arabic_matn, attribution FROM hadiths WHERE id = ?",
            arrayOf(hadithId)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                titleAgg = cursor.getString(0)
                matnAgg = cursor.getString(1)
                attribution = cursor.getString(2)
            }
        }

        val titles = StringBuilder(titleAgg)
        val matns = StringBuilder(matnAgg)
        db.rawQuery(
            "SELECT title, matn FROM hadith_translations WHERE hadith_id = ?",
            arrayOf(hadithId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                titles.append(' ').append(cursor.getString(0))
                matns.append(' ').append(cursor.getString(1))
            }
        }

        val ftsValues = ContentValues().apply {
            put("hadith_id", hadithId)
            put("title", titles.toString())
            put("matn", matns.toString())
            put("attribution", attribution)
        }
        val rowId = db.insert("hadith_fts", null, ftsValues)
        if (rowId >= 0L) {
            val mapValues = ContentValues().apply {
                put("hadith_id", hadithId)
                put("fts_rowid", rowId)
            }
            db.insertWithOnConflict("hadith_fts_map", null, mapValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun deleteFtsRowById(db: SQLiteDatabase, hadithId: String) {
        val existingRowId = db.rawQuery(
            "SELECT fts_rowid FROM hadith_fts_map WHERE hadith_id = ?",
            arrayOf(hadithId)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
        if (existingRowId != null) {
            db.delete("hadith_fts", "docid = ?", arrayOf(existingRowId.toString()))
        } else {
            db.delete("hadith_fts", "hadith_id = ?", arrayOf(hadithId))
        }
    }

    private fun findMatchingIds(db: SQLiteDatabase, query: String, limit: Int): Set<String> {
        val ids = LinkedHashSet<String>()

        val ftsQuery = buildSafeFtsMatchQuery(query)
        if (ftsQuery != null) {
            runCatching {
                db.rawQuery(
                    "SELECT DISTINCT hadith_id FROM hadith_fts WHERE hadith_fts MATCH ? LIMIT ?",
                    arrayOf(ftsQuery, limit.toString())
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getString(0))
                    }
                }
            }
        }

        if (ids.size < limit) {
            val remainingHadiths = (limit - ids.size).coerceAtLeast(1)
            val likeArg = "%${escapeLikeWildcards(query)}%"
            db.rawQuery(
                """
                SELECT DISTINCT id FROM hadiths
                WHERE arabic_title LIKE ? ESCAPE '\'
                   OR arabic_matn LIKE ? ESCAPE '\'
                   OR attribution LIKE ? ESCAPE '\'
                   OR category_title LIKE ? ESCAPE '\'
                LIMIT ?
                """.trimIndent(),
                arrayOf(likeArg, likeArg, likeArg, likeArg, remainingHadiths.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    ids.add(cursor.getString(0))
                }
            }
            if (ids.size < limit) {
                val remainingTranslations = (limit - ids.size).coerceAtLeast(1)
                db.rawQuery(
                    """
                    SELECT DISTINCT hadith_id FROM hadith_translations
                    WHERE title LIKE ? ESCAPE '\'
                       OR matn LIKE ? ESCAPE '\'
                       OR explanation LIKE ? ESCAPE '\'
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(likeArg, likeArg, likeArg, remainingTranslations.toString())
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getString(0))
                    }
                }
            }
        }

        return ids
    }

    private fun loadTranslationsMap(
        db: SQLiteDatabase,
        hadithIds: List<String>
    ): Map<String, Map<HadithLanguage, HadithTextVariant>> {
        val result = HashMap<String, MutableMap<HadithLanguage, HadithTextVariant>>()
        hadithIds.chunked(200).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val sql = """
                SELECT hadith_id, lang, title, matn, explanation, hints
                FROM hadith_translations
                WHERE hadith_id IN ($placeholders)
            """.trimIndent()
            db.rawQuery(sql, chunk.toTypedArray()).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val lang = HadithLanguage.fromCode(cursor.getString(1))
                    val hintsRaw = cursor.getString(5).orEmpty()
                    val hints = if (hintsRaw.isBlank()) emptyList() else hintsRaw.split('\n').filter { it.isNotBlank() }
                    val map = result.getOrPut(id) { LinkedHashMap() }
                    map[lang] = HadithTextVariant(
                        title = cursor.getString(2),
                        matn = cursor.getString(3),
                        explanation = cursor.getString(4),
                        hints = hints
                    )
                }
            }
        }
        return result
    }

    private fun getUserState(db: SQLiteDatabase, hadithId: String): Pair<Boolean, Boolean> {
        return db.rawQuery(
            "SELECT is_bookmarked, is_read FROM hadith_user_state WHERE hadith_id = ?",
            arrayOf(hadithId)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                Pair(cursor.getInt(0) == 1, cursor.getInt(1) == 1)
            } else {
                Pair(false, false)
            }
        }
    }

    private fun saveUserState(
        db: SQLiteDatabase,
        hadithId: String,
        isBookmarked: Boolean,
        isRead: Boolean
    ) {
        val values = ContentValues().apply {
            put("hadith_id", hadithId)
            put("is_bookmarked", if (isBookmarked) 1 else 0)
            put("is_read", if (isRead) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("hadith_user_state", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun notifyChanged() {
        _changes.value += 1L
    }

    companion object {
        private const val DATABASE_NAME = "hayya_hadith.db"
        private const val DATABASE_VERSION = 3
        const val DEFAULT_QUERY_LIMIT = 200

        private val FTS_RESERVED_KEYWORDS = setOf("AND", "OR", "NOT", "NEAR")

        internal fun buildSafeFtsMatchQuery(rawQuery: String): String? {
            val tokens = rawQuery.split(Regex("\\s+"))
                .map { token ->
                    token.replace(Regex("[^\\p{L}\\p{N}\\p{M}_]"), "").trim()
                }
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { clean ->
                if (clean.uppercase() in FTS_RESERVED_KEYWORDS) {
                    "\"$clean\""
                } else {
                    "$clean*"
                }
            }
        }

        internal fun escapeLikeWildcards(raw: String): String {
            return raw
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
        }
    }
}
