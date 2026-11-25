package dev.hayya.core.domain.update

/**
 * Semantic version representation supporting tags like `v1.0.0`, `1.2.3`, or `1.1.0-beta.1`.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val raw: String = "$major.$minor.$patch"
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // Stable release (no pre-release) is newer than a pre-release with same major.minor.patch
        if (preRelease == null && other.preRelease == null) return 0
        if (preRelease == null) return 1
        if (other.preRelease == null) return -1
        return comparePreRelease(preRelease, other.preRelease)
    }

    private fun comparePreRelease(a: String, b: String): Int {
        val partsA = a.split('.')
        val partsB = b.split('.')
        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            if (i >= partsA.size) return -1
            if (i >= partsB.size) return 1
            val partA = partsA[i]
            val partB = partsB[i]
            val numA = partA.toIntOrNull()
            val numB = partB.toIntOrNull()
            val cmp = when {
                numA != null && numB != null -> numA.compareTo(numB)
                numA != null -> -1 // Numeric identifiers have lower precedence than alphanumeric
                numB != null -> 1
                else -> partA.compareTo(partB)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }
}

/**
 * Parses and compares application version strings.
 */
object UpdateVersionComparator {

    fun parse(raw: String): AppVersion? {
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        if (cleaned.isEmpty()) return null

        val withoutBuild = cleaned.substringBefore('+')
        val dashIdx = withoutBuild.indexOf('-')
        val corePart = if (dashIdx >= 0) withoutBuild.substring(0, dashIdx) else withoutBuild
        val preRelease = if (dashIdx >= 0) withoutBuild.substring(dashIdx + 1).takeIf { it.isNotBlank() } else null

        val segments = corePart.split('.')
        if (segments.isEmpty() || segments.size > 4) return null
        val nums = segments.map { it.toIntOrNull() ?: return null }
        if (nums.any { it < 0 }) return null

        return AppVersion(
            major = nums.getOrElse(0) { 0 },
            minor = nums.getOrElse(1) { 0 },
            patch = nums.getOrElse(2) { 0 },
            preRelease = preRelease,
            raw = cleaned
        )
    }

    fun isNewerVersion(candidate: String, current: String): Boolean {
        val parsedCandidate = parse(candidate) ?: return false
        val parsedCurrent = parse(current) ?: return false
        return parsedCandidate > parsedCurrent
    }
}

/**
 * Metadata for an available GitHub release and its downloadable APK asset.
 */
data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkAssetName: String?,
    val apkSizeBytes: Long,
    val sha256AssetUrl: String? = null,
    val publishedAt: String = ""
)

/**
 * Result of checking GitHub Releases for a newer version.
 */
sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateCheckResult
    data class Failure(val message: String) : UpdateCheckResult
}

/**
 * Observable UI state for the in-app updater banner, dialog, and Settings update row.
 */
data class UpdateUiState(
    val currentVersionName: String = "1.0.0",
    val isChecking: Boolean = false,
    val availableRelease: AppReleaseInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val downloadedApkPath: String? = null,
    val needsInstallPermission: Boolean = false,
    val isUpToDateConfirmed: Boolean = false,
    val lastCheckedEpochMillis: Long? = null,
    val errorMessage: String? = null,
    val showUpdateDialog: Boolean = false
)

/**
 * Parses GitHub Releases API JSON payloads and SHA-256 checksum files.
 */
object GitHubReleaseParser {

    fun parseLatestRelease(jsonPayload: String): AppReleaseInfo? {
        val root = runCatching { SimpleJsonParser.parseObject(jsonPayload) }.getOrNull() ?: return null

        val isDraft = root["draft"] as? Boolean ?: false
        val isPrerelease = root["prerelease"] as? Boolean ?: false
        if (isDraft || isPrerelease) return null

        val tagName = (root["tag_name"] as? String)?.trim().orEmpty()
        if (tagName.isEmpty()) return null

        val parsedVersion = UpdateVersionComparator.parse(tagName) ?: return null
        val versionName = parsedVersion.raw

        val title = (root["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "Hayya $tagName"
        val releaseNotes = (root["body"] as? String)?.trim().orEmpty()
        val htmlUrl = (root["html_url"] as? String)?.trim().orEmpty()
        val publishedAt = (root["published_at"] as? String)?.trim().orEmpty()

        @Suppress("UNCHECKED_CAST")
        val rawAssets = root["assets"] as? List<Map<String, Any?>> ?: emptyList()

        val apkAsset = rawAssets
            .filter { asset ->
                val name = (asset["name"] as? String).orEmpty()
                name.endsWith(".apk", ignoreCase = true)
            }
            .sortedByDescending { asset ->
                val name = (asset["name"] as? String).orEmpty()
                if (name.contains("hayya", ignoreCase = true)) 1 else 0
            }
            .firstOrNull()

        val apkAssetName = (apkAsset?.get("name") as? String)?.trim()
        val apkDownloadUrl = (apkAsset?.get("browser_download_url") as? String)?.trim()
        val apkSizeBytes = (apkAsset?.get("size") as? Number)?.toLong() ?: 0L

        val shaAsset = rawAssets.firstOrNull { asset ->
            val name = (asset["name"] as? String).orEmpty()
            if (apkAssetName != null) {
                name.equals("$apkAssetName.sha256", ignoreCase = true) || name.endsWith(".apk.sha256", ignoreCase = true)
            } else {
                name.endsWith(".sha256", ignoreCase = true)
            }
        }
        val sha256AssetUrl = (shaAsset?.get("browser_download_url") as? String)?.trim()

        return AppReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            title = title,
            releaseNotes = releaseNotes,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkDownloadUrl,
            apkAssetName = apkAssetName,
            apkSizeBytes = apkSizeBytes,
            sha256AssetUrl = sha256AssetUrl,
            publishedAt = publishedAt
        )
    }

    fun parseSha256Hex(rawContent: String): String? {
        val token = rawContent.trim().split(Regex("\\s+")).firstOrNull()?.lowercase() ?: return null
        return if (token.length == 64 && token.all { it in '0'..'9' || it in 'a'..'f' }) {
            token
        } else {
            null
        }
    }
}
