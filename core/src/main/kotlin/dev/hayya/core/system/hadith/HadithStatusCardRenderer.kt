package dev.hayya.core.system.hadith

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import dev.hayya.core.R
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.ShareCardAspect
import dev.hayya.core.domain.hadith.ShareCardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Renders compact, clean WhatsApp Status (9:16) and Square (1:1) Hadith image cards
 * using the bundled Noto Naskh Arabic font for Arabic, Urdu, and Sindhi shaping.
 */
object HadithStatusCardRenderer {

    private data class CardPalette(
        val bgTop: Int,
        val bgBottom: Int,
        val glowColor: Int,
        val surfaceColor: Int,
        val borderColor: Int,
        val accentColor: Int,
        val primaryTextColor: Int,
        val secondaryTextColor: Int
    )

    private fun paletteFor(theme: ShareCardTheme): CardPalette = when (theme) {
        ShareCardTheme.OLED_GOLD -> CardPalette(
            bgTop = Color.parseColor("#060709"),
            bgBottom = Color.parseColor("#0D1016"),
            glowColor = Color.parseColor("#2EE7C476"),
            surfaceColor = Color.parseColor("#111318"),
            borderColor = Color.parseColor("#4CE7C476"),
            accentColor = Color.parseColor("#E7C476"),
            primaryTextColor = Color.parseColor("#F3F4F6"),
            secondaryTextColor = Color.parseColor("#9CA3AF")
        )

        ShareCardTheme.WARM_SAND -> CardPalette(
            bgTop = Color.parseColor("#F5EFE4"),
            bgBottom = Color.parseColor("#EAE0CE"),
            glowColor = Color.parseColor("#248C6200"),
            surfaceColor = Color.parseColor("#FFFCF7"),
            borderColor = Color.parseColor("#408C6200"),
            accentColor = Color.parseColor("#8C6200"),
            primaryTextColor = Color.parseColor("#181612"),
            secondaryTextColor = Color.parseColor("#574F44")
        )

        ShareCardTheme.EMERALD_NIGHT -> CardPalette(
            bgTop = Color.parseColor("#03120D"),
            bgBottom = Color.parseColor("#071D16"),
            glowColor = Color.parseColor("#2A34D399"),
            surfaceColor = Color.parseColor("#0A1D17"),
            borderColor = Color.parseColor("#4C34D399"),
            accentColor = Color.parseColor("#34D399"),
            primaryTextColor = Color.parseColor("#ECFDF5"),
            secondaryTextColor = Color.parseColor("#A7F3D0")
        )
    }

    fun renderBitmap(
        context: Context,
        entry: HadithEntry,
        preferredLanguage: HadithLanguage,
        showArabic: Boolean,
        theme: ShareCardTheme,
        aspect: ShareCardAspect
    ): Bitmap {
        val width = aspect.widthPx
        val height = aspect.heightPx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(theme)

        val arabicTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_naskh_arabic)
        }.getOrNull() ?: Typeface.DEFAULT

        // 1. Atmospheric gradient + centered ambient glow
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                palette.bgTop, palette.bgBottom,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.5f, height * 0.45f, width * 0.72f,
                palette.glowColor, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        // 2. Pre-measure body + citation so the card hugs its content cleanly
        val horizontalMargin = if (aspect == ShareCardAspect.WHATSAPP_STATUS) 76f else 64f
        val innerPadX = 64f
        val innerPadTop = 58f
        val innerPadBottom = 52f
        val cardWidth = width - (horizontalMargin * 2f)
        val contentWidth = (cardWidth - (innerPadX * 2f)).toInt().coerceAtLeast(300)

        val minVerticalMargin = if (aspect == ShareCardAspect.WHATSAPP_STATUS) 180f else 64f
        val maxCardHeight = height - (minVerticalMargin * 2f)
        val minCardHeight = if (aspect == ShareCardAspect.WHATSAPP_STATUS) 740f else 620f

        val headerHeight = 56f // subtle accent bar + category label
        val citationText = buildString {
            append(entry.attribution.ifBlank { "Authenticated Hadith" })
            if (entry.grade.isNotBlank()) {
                append("  •  ")
                append(entry.grade)
            }
        }
        val citationPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryTextColor
            textSize = 25f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val citationLayout = buildStaticLayout(citationText, citationPaint, contentWidth, lineMult = 1.15f)
        val footerHeight = 28f + citationLayout.height

        val availableBodyMax = (maxCardHeight - innerPadTop - innerPadBottom - headerHeight - footerHeight - 64f)
            .toInt()
            .coerceAtLeast(220)

        val resolved = entry.resolveTranslation(preferredLanguage)
        val translationText = resolved.variant.matn
        val arabicText = if (showArabic && entry.arabic.matn.isNotBlank()) entry.arabic.matn else ""
        val translationTypeface = if (resolved.language.isRtl) arabicTypeface else Typeface.SANS_SERIF

        var scale = 1.0f
        var arabicLayout: StaticLayout?
        var translationLayout: StaticLayout
        val baseArabicSize = if (aspect == ShareCardAspect.WHATSAPP_STATUS) 42f else 34f
        val baseTransSize = if (aspect == ShareCardAspect.WHATSAPP_STATUS) 36f else 28f

        while (true) {
            val arabicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentColor
                textSize = (baseArabicSize * scale).coerceAtLeast(22f)
                typeface = arabicTypeface
            }
            val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.primaryTextColor
                textSize = (baseTransSize * scale).coerceAtLeast(19f)
                typeface = translationTypeface
            }

            val aLayout = if (arabicText.isNotEmpty()) {
                buildStaticLayout(arabicText, arabicPaint, contentWidth, lineMult = 1.32f)
            } else {
                null
            }
            val tLayout = buildStaticLayout(translationText, transPaint, contentWidth, lineMult = 1.28f)
            val gap = if (aLayout != null) 30 else 0
            val totalHeight = (aLayout?.height ?: 0) + gap + tLayout.height
            if (totalHeight <= availableBodyMax || scale <= 0.44f) {
                arabicLayout = aLayout
                translationLayout = tLayout
                break
            }
            scale -= 0.05f
        }

        val bodyGap = if (arabicLayout != null) 30 else 0
        val bodyHeight = (arabicLayout?.height ?: 0) + bodyGap + translationLayout.height
        val naturalCardHeight = innerPadTop + headerHeight + 34f + bodyHeight + 34f + footerHeight + innerPadBottom
        val cardHeight = naturalCardHeight.coerceIn(minCardHeight, maxCardHeight)
        val cardTop = (height - cardHeight) / 2f
        val cardRect = RectF(horizontalMargin, cardTop, width - horizontalMargin, cardTop + cardHeight)

        // 3. Draw sleek single-surface card with crisp hairline border
        val cardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.surfaceColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardFill)

        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.borderColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBorder)

        val contentLeft = cardRect.left + innerPadX

        // 4. Subtle top accent bar + Pillar label
        val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentColor
        }
        val barW = 56f
        val barH = 5f
        val barTop = cardRect.top + innerPadTop
        canvas.drawRoundRect(
            RectF(cardRect.centerX() - barW / 2f, barTop, cardRect.centerX() + barW / 2f, barTop + barH),
            3f,
            3f,
            accentBarPaint
        )

        val topicPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentColor
            textSize = 23f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.10f
        }
        val topicLabel = entry.pillar.title.uppercase()
        val topicW = topicPaint.measureText(topicLabel)
        canvas.drawText(
            topicLabel,
            cardRect.centerX() - topicW / 2f,
            barTop + barH + 34f,
            topicPaint
        )

        // 5. Centered Arabic + Translation body
        val bodyAreaTop = barTop + headerHeight + 28f
        val bodyAreaBottom = cardRect.bottom - innerPadBottom - footerHeight - 28f
        val bodyAreaHeight = (bodyAreaBottom - bodyAreaTop).coerceAtLeast(bodyHeight.toFloat())
        var cursorY = bodyAreaTop + ((bodyAreaHeight - bodyHeight).coerceAtLeast(0f) / 2f)

        if (arabicLayout != null) {
            canvas.save()
            canvas.translate(contentLeft, cursorY)
            arabicLayout.draw(canvas)
            canvas.restore()
            cursorY += arabicLayout.height + bodyGap
        }

        canvas.save()
        canvas.translate(contentLeft, cursorY)
        translationLayout.draw(canvas)
        canvas.restore()

        // 6. Clean citation footer (no promotional bottom CTA)
        val citationY = cardRect.bottom - innerPadBottom - citationLayout.height
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.borderColor
            strokeWidth = 1.5f
        }
        val dividerY = citationY - 22f
        canvas.drawLine(
            cardRect.centerX() - 80f,
            dividerY,
            cardRect.centerX() + 80f,
            dividerY,
            dividerPaint
        )

        canvas.save()
        canvas.translate(contentLeft, citationY)
        citationLayout.draw(canvas)
        canvas.restore()

        return bitmap
    }

    suspend fun exportPngForSharing(
        context: Context,
        entry: HadithEntry,
        preferredLanguage: HadithLanguage,
        showArabic: Boolean,
        theme: ShareCardTheme,
        aspect: ShareCardAspect
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "shared_cards").apply {
                if (!exists()) mkdirs()
            }
            dir.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }

            val safeId = entry.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val outputFile = File(dir, "hayya_hadith_${safeId}.png")
            val bitmap = renderBitmap(
                context = context,
                entry = entry,
                preferredLanguage = preferredLanguage,
                showArabic = showArabic,
                theme = theme,
                aspect = aspect
            )
            try {
                FileOutputStream(outputFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                }
            } finally {
                bitmap.recycle()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        }
    }

    fun buildShareImageIntent(imageUri: Uri, entry: HadithEntry, language: HadithLanguage): Intent {
        val resolved = entry.resolveTranslation(language)
        val caption = "${resolved.variant.title}\n— ${entry.attribution}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share Hadith Status Card").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun buildStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        lineMult: Float = 1.2f
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, lineMult)
            .setIncludePad(false)
            .build()
    }
}
