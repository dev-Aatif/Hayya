package dev.hayya.core.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hayya.core.domain.hadith.HadithBookInfo
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithUserPreferences
import dev.hayya.core.domain.hadith.ShareCardAspect
import dev.hayya.core.domain.hadith.ShareCardTheme
import dev.hayya.core.system.hadith.HadithStatusCardRenderer
import dev.hayya.core.ui.theme.ArabicScriptFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HadithReaderModalSheet(
    entry: HadithEntry,
    preferences: HadithUserPreferences,
    onSelectLanguage: (HadithLanguage) -> Unit,
    onToggleShowArabic: () -> Unit,
    onAdjustFontScale: (Float) -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenShareSheet: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val resolved = remember(entry, preferences.preferredLanguage) {
        entry.resolveTranslation(preferences.preferredLanguage)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Toolbar: language pills + font controls + actions (wraps gracefully at 320dp / high fontScale)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HadithLanguage.entries.forEach { lang ->
                        val selected = preferences.preferredLanguage == lang
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectLanguage(lang) },
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        ) {
                            Text(
                                text = lang.nativeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAdjustFontScale(-0.1f) }) {
                        Text(
                            text = "A-",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    IconButton(onClick = { onAdjustFontScale(0.1f) }) {
                        Text(
                            text = "A+",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (entry.isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark Hadith",
                            tint = if (entry.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReaderChip(text = entry.pillar.title, highlight = true)
                if (entry.grade.isNotBlank()) {
                    ReaderChip(text = entry.grade, highlight = false)
                }
                if (resolved.isFallback) {
                    ReaderChip(
                        text = "${preferences.preferredLanguage.displayName} unavailable • Showing ${resolved.language.displayName}",
                        highlight = false
                    )
                }
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleShowArabic),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = if (preferences.showArabic) "Arabic: On" else "Arabic: Off",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Hadith body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (resolved.variant.title.isNotBlank()) {
                    val titleDir = if (resolved.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides titleDir) {
                        Text(
                            text = resolved.variant.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = if (resolved.language.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.titleMedium.fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (preferences.showArabic && entry.arabic.matn.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = entry.arabic.matn,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = ArabicScriptFontFamily,
                                    fontSize = (20f * preferences.fontScale).sp,
                                    lineHeight = (34f * preferences.fontScale).sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val transDir = if (resolved.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides transDir) {
                    Text(
                        text = resolved.variant.matn,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (resolved.language.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.bodyLarge.fontFamily,
                            fontSize = (16f * preferences.fontScale).sp,
                            lineHeight = (27f * preferences.fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (resolved.variant.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Reflection & Meaning",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides transDir) {
                        Text(
                            text = resolved.variant.explanation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = if (resolved.language.isRtl) ArabicScriptFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
                                fontSize = (14f * preferences.fontScale).sp,
                                lineHeight = (23f * preferences.fontScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Source: ${entry.attribution}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: Copy Text & Share WhatsApp Status Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        copyHadithToClipboard(context, entry, preferences)
                        Toast.makeText(context, "Hadith copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onOpenShareSheet,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Status Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HadithShareSheet(
    entry: HadithEntry,
    preferredLanguage: HadithLanguage,
    initialShowArabic: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTheme by remember { mutableStateOf(ShareCardTheme.OLED_GOLD) }
    var selectedAspect by remember { mutableStateOf(ShareCardAspect.WHATSAPP_STATUS) }
    var includeArabic by remember { mutableStateOf(initialShowArabic) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(entry.id, preferredLanguage, selectedTheme, selectedAspect, includeArabic) {
        val rendered = withContext(Dispatchers.Default) {
            HadithStatusCardRenderer.renderBitmap(
                context = context,
                entry = entry,
                preferredLanguage = preferredLanguage,
                showArabic = includeArabic,
                theme = selectedTheme,
                aspect = selectedAspect
            )
        }
        previewBitmap = rendered
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        val triggerShare: () -> Unit = {
            if (!isExporting) {
                isExporting = true
                scope.launch {
                    val result = HadithStatusCardRenderer.exportPngForSharing(
                        context = context,
                        entry = entry,
                        preferredLanguage = preferredLanguage,
                        showArabic = includeArabic,
                        theme = selectedTheme,
                        aspect = selectedAspect
                    )
                    isExporting = false
                    result.fold(
                        onSuccess = { uri ->
                            runCatching {
                                val chooser = HadithStatusCardRenderer.buildShareImageIntent(
                                    imageUri = uri,
                                    entry = entry,
                                    language = preferredLanguage
                                )
                                context.startActivity(chooser)
                            }.onFailure {
                                Toast.makeText(context, "No share target available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { err ->
                            Toast.makeText(
                                context,
                                err.message ?: "Failed to create card",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact Header with Top-Right Share Pill (No bottom CTA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Status Card",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Tap card or Share for WhatsApp Status",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !isExporting, onClick = triggerShare),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (isExporting) "Sharing…" else "Share",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Card Preview (tap to share)
            val aspectRatio = selectedAspect.widthPx.toFloat() / selectedAspect.heightPx.toFloat()
            Box(
                modifier = Modifier
                    .height(290.dp)
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .clickable(enabled = !isExporting, onClick = triggerShare),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Hadith card preview",
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Single Compact Control Bar: Theme Swatches + Aspect + Arabic Toggle
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShareCardTheme.entries.forEach { theme ->
                        val selected = selectedTheme == theme
                        val swatchColor = when (theme) {
                            ShareCardTheme.OLED_GOLD -> androidx.compose.ui.graphics.Color(0xFF0E1117)
                            ShareCardTheme.WARM_SAND -> androidx.compose.ui.graphics.Color(0xFFF5EFE4)
                            ShareCardTheme.EMERALD_NIGHT -> androidx.compose.ui.graphics.Color(0xFF072219)
                        }
                        val accentColor = when (theme) {
                            ShareCardTheme.OLED_GOLD -> androidx.compose.ui.graphics.Color(0xFFE7C476)
                            ShareCardTheme.WARM_SAND -> androidx.compose.ui.graphics.Color(0xFF8C6200)
                            ShareCardTheme.EMERALD_NIGHT -> androidx.compose.ui.graphics.Color(0xFF34D399)
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { selectedTheme = theme }
                                .semantics {
                                    role = Role.RadioButton
                                    contentDescription = "Card theme ${theme.label}"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextAspect = if (selectedAspect == ShareCardAspect.WHATSAPP_STATUS) {
                        ShareCardAspect.SQUARE
                    } else {
                        ShareCardAspect.WHATSAPP_STATUS
                    }
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedAspect = nextAspect },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = if (selectedAspect == ShareCardAspect.WHATSAPP_STATUS) "9:16" else "1:1",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { includeArabic = !includeArabic },
                        shape = CircleShape,
                        color = if (includeArabic) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (includeArabic) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Text(
                            text = if (includeArabic) "عربي ✓" else "عربي",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ArabicScriptFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (includeArabic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HadithSyncSheet(
    categories: List<HadithCategorySyncInfo>,
    books: List<HadithBookInfo>,
    isLoadingCategories: Boolean,
    syncingCategoryId: String?,
    downloadingBookId: String?,
    syncProgressText: String?,
    syncError: String?,
    onRefreshCategories: () -> Unit,
    onSyncCategory: (HadithCategorySyncInfo) -> Unit,
    onDownloadBook: (HadithBookInfo) -> Unit,
    onOpenBook: (String) -> Unit,
    onDeleteBook: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBooksTab by remember { mutableStateOf(true) }
    val isBusy = syncingCategoryId != null || downloadingBookId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Library",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (showBooksTab) {
                            "Full canonical books (Arabic, English & Urdu)"
                        } else {
                            "Prioritized spiritual & tafsir topics (AR, EN, UR, SD)"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                if (!showBooksTab) {
                    IconButton(
                        onClick = onRefreshCategories,
                        enabled = !isLoadingCategories && !isBusy
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh topics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Tab Selector: Canonical Books vs HadeethEnc Topics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(true to "Canonical Books", false to "Topics (with Sindhi)").forEach { (isBooks, label) ->
                    val selected = showBooksTab == isBooks
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { showBooksTab = isBooks },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            if (syncProgressText != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = syncProgressText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            if (syncError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = syncError,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.error
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showBooksTab) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(books, key = { it.bookId }) { book ->
                        val isDownloadingThis = downloadingBookId == book.bookId
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                if (book.isDownloaded) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (book.isDownloaded) {
                                            "${book.downloadedCount} Hadiths offline (${book.downloadedLanguages.joinToString("/")})"
                                        } else {
                                            book.subtitle
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (book.isDownloaded) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (book.isDownloaded) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable(enabled = !isBusy) {
                                                    onOpenBook(book.bookId)
                                                    onDismiss()
                                                },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "Read",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteBook(book.bookId) },
                                            enabled = !isBusy
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Remove offline book",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable(enabled = !isBusy) { onDownloadBook(book) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (isDownloadingThis) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.CloudDownload,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isDownloadingThis) "…" else "Download",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when {
                    isLoadingCategories && categories.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    categories.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap refresh to load online topic packs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(categories, key = { it.id }) { cat ->
                                val isThisSyncing = syncingCategoryId == cat.id
                                val allSynced = cat.remoteCount > 0 && cat.syncedCount >= cat.remoteCount
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cat.pillar.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Text(
                                                text = cat.title,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${cat.syncedCount} / ${cat.remoteCount} saved offline",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable(enabled = !isBusy && !allSynced) {
                                                    onSyncCategory(cat)
                                                },
                                            shape = CircleShape,
                                            color = if (allSynced) {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                if (isThisSyncing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CloudDownload,
                                                        contentDescription = null,
                                                        tint = if (allSynced) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        } else {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        },
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                Text(
                                                    text = when {
                                                        allSynced -> "Saved"
                                                        cat.syncedCount > 0 -> "+20"
                                                        else -> "Get 20"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (allSynced) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        } else {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderChip(
    text: String,
    highlight: Boolean
) {
    Surface(
        shape = CircleShape,
        color = if (highlight) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun copyHadithToClipboard(
    context: Context,
    entry: HadithEntry,
    preferences: HadithUserPreferences
) {
    val resolved = entry.resolveTranslation(preferences.preferredLanguage)
    val text = buildString {
        if (preferences.showArabic && entry.arabic.matn.isNotBlank()) {
            appendLine(entry.arabic.matn)
            appendLine()
        }
        appendLine(resolved.variant.matn)
        appendLine()
        append("— ${entry.attribution}")
        if (entry.grade.isNotBlank()) {
            append(" (${entry.grade})")
        }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Hadith", text))
}
