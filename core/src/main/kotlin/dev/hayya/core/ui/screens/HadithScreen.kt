package dev.hayya.core.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.data.hadith.HadithStarterPack
import dev.hayya.core.domain.hadith.HadithBookInfo
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithFeedFilter
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.ui.theme.ArabicScriptFontFamily
import dev.hayya.core.ui.theme.HayyaSuccess
import dev.hayya.core.ui.theme.HayyaTheme
import dev.hayya.core.ui.viewmodel.HadithUiState

/**
 * Primary Hadith Hub featuring multilingual support (Arabic, English, Urdu, Sindhi),
 * prioritized spiritual/worship/creed/manners/tafsir filtering, offline SQLite search,
 * WhatsApp Status share card generation, and on-demand HadeethEnc topic packs.
 */
@Composable
fun HadithScreen(
    state: HadithUiState,
    modifier: Modifier = Modifier,
    onSelectLanguage: (HadithLanguage) -> Unit = {},
    onToggleShowArabic: () -> Unit = {},
    onAdjustFontScale: (Float) -> Unit = {},
    onSelectFilter: (HadithFeedFilter) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onOpenHadith: (HadithEntry) -> Unit = {},
    onCloseHadith: () -> Unit = {},
    onToggleBookmark: (String) -> Unit = {},
    onOpenShareSheet: (HadithEntry) -> Unit = {},
    onCloseShareSheet: () -> Unit = {},
    onOpenSyncSheet: () -> Unit = {},
    onCloseSyncSheet: () -> Unit = {},
    onRefreshCategories: () -> Unit = {},
    onSyncCategory: (HadithCategorySyncInfo) -> Unit = {},
    onSelectBook: (String?) -> Unit = {},
    onDownloadBook: (HadithBookInfo) -> Unit = {},
    onDeleteBook: (String) -> Unit = {},
    onToggleMorningNotification: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header + Offline Packs Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hadith",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "${state.totalStoredCount} saved offline • Spiritual & Worship Focus",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val morningOn = state.preferences.morningNotificationEnabled
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleMorningNotification),
                        shape = CircleShape,
                        color = if (morningOn) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            if (morningOn) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (morningOn) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsOff
                                },
                                contentDescription = "Toggle sunrise Hadith notification",
                                tint = if (morningOn) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sunrise",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (morningOn) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSyncSheet),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = "Download topic packs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Packs",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Downloaded Books Quick Switcher (shown whenever at least 1 canonical book is downloaded)
            val downloadedBooks = remember(state.books) { state.books.filter { it.isDownloaded } }
            if (downloadedBooks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "curated_feed") {
                        val selected = state.selectedBookId == null
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectBook(null) },
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ) {
                            Text(
                                text = "Curated Feed",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(downloadedBooks, key = { it.bookId }) { book ->
                        val selected = state.selectedBookId == book.bookId
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectBook(book.bookId) },
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Language Switcher Row (English / Urdu / Sindhi + Arabic toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HadithLanguage.entries.forEach { lang ->
                    val selected = state.preferences.preferredLanguage == lang
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectLanguage(lang) },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            }
                        )
                    ) {
                        Text(
                            text = lang.nativeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = if (lang.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.labelMedium.fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleShowArabic),
                    shape = CircleShape,
                    color = if (state.preferences.showArabic) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        if (state.preferences.showArabic) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }
                    )
                ) {
                    Text(
                        text = if (state.preferences.showArabic) "عربي ✓" else "عربي",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ArabicScriptFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (state.preferences.showArabic) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Search Hadith, Urdu, Sindhi, or narrator…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Topic Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HadithFeedFilter.entries) { filter ->
                    val selected = state.activeFilter == filter
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectFilter(filter) },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                            }
                        )
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Feed List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.selectedBookId == null &&
                    state.activeFilter == HadithFeedFilter.ALL &&
                    state.searchQuery.isBlank() &&
                    state.dailyHadith != null
                ) {
                    item(key = "daily_hero") {
                        DailyHadithHeroCard(
                            entry = state.dailyHadith,
                            preferredLanguage = state.preferences.preferredLanguage,
                            showArabic = state.preferences.showArabic,
                            onReadClick = { onOpenHadith(state.dailyHadith) },
                            onToggleBookmark = { onToggleBookmark(state.dailyHadith.id) },
                            onShareClick = { onOpenShareSheet(state.dailyHadith) }
                        )
                    }
                }

                if (state.entries.isEmpty() && state.isLoaded) {
                    item(key = "empty_state") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (state.activeFilter == HadithFeedFilter.SAVED) {
                                        "No saved Hadiths yet"
                                    } else {
                                        "No matching Hadiths found"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (state.activeFilter == HadithFeedFilter.SAVED) {
                                        "Bookmark any Hadith to keep it ready here offline."
                                    } else {
                                        "Try another keyword or download more offline topic packs."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                } else {
                    items(state.entries, key = { it.id }) { entry ->
                        HadithFeedCard(
                            entry = entry,
                            preferredLanguage = state.preferences.preferredLanguage,
                            showArabic = state.preferences.showArabic,
                            onClick = { onOpenHadith(entry) },
                            onToggleBookmark = { onToggleBookmark(entry.id) },
                            onShareClick = { onOpenShareSheet(entry) }
                        )
                    }
                }

                item(key = "attribution_footer") {
                    Text(
                        text = "Authenticated thematic content powered by HadeethEnc.com",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }

    state.selectedHadith?.let { openEntry ->
        HadithReaderModalSheet(
            entry = openEntry,
            preferences = state.preferences,
            onSelectLanguage = onSelectLanguage,
            onToggleShowArabic = onToggleShowArabic,
            onAdjustFontScale = onAdjustFontScale,
            onToggleBookmark = { onToggleBookmark(openEntry.id) },
            onOpenShareSheet = { onOpenShareSheet(openEntry) },
            onDismiss = onCloseHadith
        )
    }

    state.shareTargetHadith?.let { shareEntry ->
        HadithShareSheet(
            entry = shareEntry,
            preferredLanguage = state.preferences.preferredLanguage,
            initialShowArabic = state.preferences.showArabic,
            onDismiss = onCloseShareSheet
        )
    }

    if (state.isSyncSheetOpen) {
        HadithSyncSheet(
            categories = state.categories,
            books = state.books,
            isLoadingCategories = state.isLoadingCategories,
            syncingCategoryId = state.syncingCategoryId,
            downloadingBookId = state.downloadingBookId,
            syncProgressText = state.syncProgressText,
            syncError = state.syncError,
            onRefreshCategories = onRefreshCategories,
            onSyncCategory = onSyncCategory,
            onDownloadBook = onDownloadBook,
            onOpenBook = { bookId -> onSelectBook(bookId) },
            onDeleteBook = onDeleteBook,
            onDismiss = onCloseSyncSheet
        )
    }
}

@Composable
private fun DailyHadithHeroCard(
    entry: HadithEntry,
    preferredLanguage: HadithLanguage,
    showArabic: Boolean,
    onReadClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShareClick: () -> Unit
) {
    val resolved = remember(entry, preferredLanguage) {
        entry.resolveTranslation(preferredLanguage)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onReadClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "TODAY'S HADITH • ${entry.pillar.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (entry.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Status Card",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (showArabic && entry.arabic.matn.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = entry.arabic.matn,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = ArabicScriptFontFamily,
                            fontSize = 18.sp,
                            lineHeight = 29.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val transDir = if (resolved.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides transDir) {
                Text(
                    text = resolved.variant.matn,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = if (resolved.language.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
                        lineHeight = if (resolved.language.isRtl) 25.sp else 21.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.attribution,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to read",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun HadithFeedCard(
    entry: HadithEntry,
    preferredLanguage: HadithLanguage,
    showArabic: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShareClick: () -> Unit
) {
    val resolved = remember(entry, preferredLanguage) {
        entry.resolveTranslation(preferredLanguage)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = entry.pillar.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (entry.grade.isNotBlank()) {
                        Text(
                            text = entry.grade,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (entry.isRead) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Read",
                            tint = HayyaSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (entry.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (entry.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Status Card",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (resolved.variant.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val titleDir = if (resolved.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides titleDir) {
                    Text(
                        text = resolved.variant.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = if (resolved.language.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.titleSmall.fontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (showArabic && entry.arabic.matn.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = entry.arabic.matn,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ArabicScriptFontFamily,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val transDir = if (resolved.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides transDir) {
                Text(
                    text = resolved.variant.matn,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = if (resolved.language.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
                        lineHeight = if (resolved.language.isRtl) 24.sp else 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.attribution,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Hadith Hub Dark", showBackground = true)
@Composable
private fun HadithScreenDarkPreview() {
    HayyaTheme(themeMode = ThemeModeOption.DARK) {
        HadithScreen(
            state = HadithUiState(
                isLoaded = true,
                dailyHadith = HadithStarterPack.entries.first(),
                entries = HadithStarterPack.entries.take(4),
                totalStoredCount = HadithStarterPack.entries.size
            )
        )
    }
}

@Preview(name = "Hadith Hub Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun HadithScreenLightPreview() {
    HayyaTheme(themeMode = ThemeModeOption.LIGHT) {
        HadithScreen(
            state = HadithUiState(
                isLoaded = true,
                dailyHadith = HadithStarterPack.entries.first(),
                entries = HadithStarterPack.entries.take(4),
                totalStoredCount = HadithStarterPack.entries.size
            )
        )
    }
}
