package dev.hayya.core.system.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.hayya.core.R
import dev.hayya.core.domain.update.AppReleaseInfo
import dev.hayya.core.domain.update.GitHubReleaseParser
import dev.hayya.core.domain.update.UpdateCheckResult
import dev.hayya.core.domain.update.UpdateVersionComparator
import dev.hayya.core.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Abstraction for network calls performed by [AppUpdateEngine], enabling deterministic unit testing.
 */
interface UpdateHttpTransport {
    suspend fun getText(url: String): Result<String>
    suspend fun downloadToFile(url: String, destinationFile: File, onProgress: (Int) -> Unit): Result<Unit>
}

/**
 * [HttpURLConnection]-backed implementation with explicit timeouts, HTTPS enforcement,
 * and redirect handling for GitHub Releases and CDN asset downloads.
 */
class HttpUrlConnectionUpdateTransport(
    private val userAgent: String = "Hayya-Android"
) : UpdateHttpTransport {

    override suspend fun getText(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openFollowingRedirects(
                initialUrl = url,
                acceptHeader = "application/vnd.github+json, application/json;q=0.9"
            )
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code while checking updates")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun downloadToFile(
        url: String,
        destinationFile: File,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val partFile = File(parent, "${destinationFile.name}.part")
        if (partFile.exists()) {
            partFile.delete()
        }

        try {
            val connection = openFollowingRedirects(
                initialUrl = url,
                acceptHeader = "application/octet-stream"
            )
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("HTTP $code while downloading update"))
                }

                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L
                var lastReportedPercent = -1
                onProgress(0)

                connection.inputStream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0L) {
                                val percent = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (percent != lastReportedPercent) {
                                    lastReportedPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (downloadedBytes <= 0L) {
                    partFile.delete()
                    return@withContext Result.failure(IllegalStateException("Downloaded APK file is empty"))
                }

                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                if (!partFile.renameTo(destinationFile)) {
                    partFile.copyTo(destinationFile, overwrite = true)
                    partFile.delete()
                }
                onProgress(100)
                Result.success(Unit)
            } finally {
                connection.disconnect()
            }
        } catch (t: Throwable) {
            partFile.delete()
            Result.failure(t)
        }
    }

    private fun openFollowingRedirects(initialUrl: String, acceptHeader: String): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (true) {
            require(currentUrl.startsWith("https://", ignoreCase = true)) {
                "Refusing non-HTTPS update URL"
            }
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", acceptHeader)
                setRequestProperty("User-Agent", userAgent)
            }
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect missing Location header")
                }
                if (++redirects > MAX_REDIRECTS) {
                    throw IllegalStateException("Too many redirects while fetching update")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
                continue
            }
            return conn
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 20_000
        const val MAX_REDIRECTS = 5
    }
}

/**
 * GitHub Releases updater engine that checks for newer releases, posts Android notifications,
 * downloads & SHA-256-verifies release APKs into app cache, and launches the system package installer.
 */
class AppUpdateEngine(
    private val context: Context,
    private val httpTransport: UpdateHttpTransport = HttpUrlConnectionUpdateTransport(),
    private val repoOwner: String = DEFAULT_REPO_OWNER,
    private val repoName: String = DEFAULT_REPO_NAME,
    private val currentVersionProvider: () -> String = { resolveInstalledVersionName(context) }
) {

    fun currentVersionName(): String = currentVersionProvider()

    fun shouldAutoCheckOnLaunch(lastCheckedEpochMillis: Long, nowEpochMillis: Long): Boolean {
        if (lastCheckedEpochMillis <= 0L) return true
        return (nowEpochMillis - lastCheckedEpochMillis) >= AUTO_CHECK_INTERVAL_MILLIS
    }

    suspend fun checkForUpdate(currentVersion: String? = null): UpdateCheckResult {
        val resolvedCurrentVersion = currentVersion ?: currentVersionProvider()
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val jsonResult = httpTransport.getText(apiUrl)
        val payload = jsonResult.getOrElse { error ->
            return UpdateCheckResult.Failure(error.message ?: "Unable to reach GitHub Releases")
        }

        val release = GitHubReleaseParser.parseLatestRelease(payload)
            ?: return UpdateCheckResult.Failure("No published release metadata found")

        return if (UpdateVersionComparator.isNewerVersion(release.versionName, resolvedCurrentVersion)) {
            UpdateCheckResult.UpdateAvailable(release)
        } else {
            UpdateCheckResult.UpToDate(resolvedCurrentVersion)
        }
    }

    suspend fun downloadAndVerifyApk(
        release: AppReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<File> {
        val downloadUrl = release.apkDownloadUrl
            ?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?: return Result.failure(IllegalStateException("Release does not contain a valid HTTPS APK download"))

        val updatesDir = File(context.cacheDir, UPDATES_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        // Clean up any stale APK or partial downloads from previous versions
        updatesDir.listFiles()?.forEach { existing ->
            if (existing.isFile) existing.delete()
        }

        val sanitizedName = sanitizeApkFileName(release.apkAssetName ?: "Hayya-${release.versionName}.apk")
        val targetFile = File(updatesDir, sanitizedName)

        val downloadResult = httpTransport.downloadToFile(downloadUrl, targetFile, onProgress)
        if (downloadResult.isFailure) {
            targetFile.delete()
            return Result.failure(downloadResult.exceptionOrNull() ?: IllegalStateException("Download failed"))
        }

        val shaUrl = release.sha256AssetUrl?.takeIf { it.startsWith("https://", ignoreCase = true) }
        if (shaUrl != null) {
            val shaText = httpTransport.getText(shaUrl).getOrElse { error ->
                targetFile.delete()
                return Result.failure(
                    IllegalStateException("Unable to verify APK integrity (SHA-256 fetch failed): ${error.message ?: "network error"}")
                )
            }
            val expectedHex = GitHubReleaseParser.parseSha256Hex(shaText)
            if (expectedHex == null) {
                targetFile.delete()
                return Result.failure(IllegalStateException("Release SHA-256 checksum file is invalid"))
            }
            val actualHex = withContext(Dispatchers.IO) { computeFileSha256(targetFile) }
            if (!actualHex.equals(expectedHex, ignoreCase = true)) {
                targetFile.delete()
                return Result.failure(IllegalStateException("APK integrity check (SHA-256) failed"))
            }
        }

        return Result.success(targetFile)
    }

    fun canRequestPackageInstalls(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun createUnknownSourcesSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchApkInstaller(apkFile: File): Result<Unit> = runCatching {
        require(apkFile.exists() && apkFile.length() > 0L) { "Downloaded APK file is missing" }
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun notifyUpdateAvailable(release: AppReleaseInfo) {
        ensureUpdateNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping update notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle("Hayya ${release.versionName} Available")
            .setContentText("A new update is ready to download and install.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Hayya v${release.versionName} is now available on GitHub Releases. Tap to review and install."
                )
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE, notification)
    }

    companion object {
        private const val TAG = "AppUpdateEngine"
        const val DEFAULT_REPO_OWNER = "dev-Aatif"
        const val DEFAULT_REPO_NAME = "Hayya"
        const val CHANNEL_APP_UPDATES = "channel_app_updates"
        const val NOTIFICATION_ID_UPDATE = 9401
        const val EXTRA_OPEN_UPDATE_DIALOG = "dev.hayya.extra.OPEN_UPDATE_DIALOG"
        const val AUTO_CHECK_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L
        private const val UPDATES_CACHE_DIR = "updates"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        fun resolveInstalledVersionName(context: Context): String {
            return runCatching {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                info.versionName?.takeIf { it.isNotBlank() } ?: "1.0.0"
            }.getOrDefault("1.0.0")
        }

        fun sanitizeApkFileName(rawName: String): String {
            val base = rawName.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .trimStart('.')
            val nonEmpty = base.ifBlank { "Hayya-update.apk" }
            return if (nonEmpty.endsWith(".apk", ignoreCase = true)) nonEmpty else "$nonEmpty.apk"
        }

        fun computeFileSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun ensureUpdateNotificationChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_APP_UPDATES,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when a new version of Hayya is available"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
