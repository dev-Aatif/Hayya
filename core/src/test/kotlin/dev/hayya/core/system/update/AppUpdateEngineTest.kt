package dev.hayya.core.system.update

import android.content.Context
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.update.AppReleaseInfo
import dev.hayya.core.domain.update.UpdateCheckResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class AppUpdateEngineTest {

    private lateinit var cacheDir: File
    private lateinit var context: Context

    @Before
    fun setUp() {
        cacheDir = File.createTempFile("hayya_update_cache", "").apply {
            delete()
            mkdirs()
        }
        context = mockk(relaxed = true) {
            every { this@mockk.cacheDir } returns this@AppUpdateEngineTest.cacheDir
            every { packageName } returns "dev.hayya"
        }
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun checkForUpdate_returnsUpdateAvailable_whenRemoteVersionIsNewer() = runTest {
        val transport = FakeTransport(
            textResponses = mapOf(
                LATEST_URL to """
                    {
                      "tag_name": "v1.1.0",
                      "name": "Hayya v1.1.0",
                      "draft": false,
                      "prerelease": false,
                      "html_url": "https://github.com/dev-Aatif/Hayya/releases/tag/v1.1.0",
                      "body": "Hadith tab preview",
                      "assets": [
                        {
                          "name": "Hayya-v1.1.0.apk",
                          "size": 1024,
                          "browser_download_url": "https://github.com/dev-Aatif/Hayya/releases/download/v1.1.0/Hayya-v1.1.0.apk"
                        }
                      ]
                    }
                """.trimIndent()
            )
        )
        val engine = AppUpdateEngine(
            context = context,
            httpTransport = transport,
            currentVersionProvider = { "1.0.0" }
        )

        val result = engine.checkForUpdate()
        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        val available = result as UpdateCheckResult.UpdateAvailable
        assertThat(available.release.versionName).isEqualTo("1.1.0")
        assertThat(available.release.apkDownloadUrl).isNotNull()
    }

    @Test
    fun checkForUpdate_returnsUpToDate_whenCurrentMatchesLatest() = runTest {
        val transport = FakeTransport(
            textResponses = mapOf(
                LATEST_URL to """{"tag_name": "v1.0.0", "draft": false, "prerelease": false, "assets": []}"""
            )
        )
        val engine = AppUpdateEngine(
            context = context,
            httpTransport = transport,
            currentVersionProvider = { "1.0.0" }
        )

        val result = engine.checkForUpdate()
        assertThat(result).isEqualTo(UpdateCheckResult.UpToDate("1.0.0"))
    }

    @Test
    fun checkForUpdate_returnsFailure_whenTransportFails() = runTest {
        val transport = FakeTransport(
            textError = IllegalStateException("Network offline")
        )
        val engine = AppUpdateEngine(
            context = context,
            httpTransport = transport,
            currentVersionProvider = { "1.0.0" }
        )

        val result = engine.checkForUpdate()
        assertThat(result).isInstanceOf(UpdateCheckResult.Failure::class.java)
        assertThat((result as UpdateCheckResult.Failure).message).contains("Network offline")
    }

    @Test
    fun shouldAutoCheckOnLaunch_throttlesWithinInterval() {
        val engine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(),
            currentVersionProvider = { "1.0.0" }
        )
        val now = 1_000_000_000L

        assertThat(engine.shouldAutoCheckOnLaunch(0L, now)).isTrue()
        assertThat(engine.shouldAutoCheckOnLaunch(now - 60_000L, now)).isFalse()
        assertThat(engine.shouldAutoCheckOnLaunch(now - AppUpdateEngine.AUTO_CHECK_INTERVAL_MILLIS, now)).isTrue()
    }

    @Test
    fun downloadAndVerifyApk_verifiesSha256AndCleansUpOnMismatch() = runTest {
        val apkBytes = "fake-apk-binary-payload".toByteArray()
        val digestHex = MessageDigest.getInstance("SHA-256")
            .digest(apkBytes)
            .joinToString("") { "%02x".format(it) }

        val shaUrl = "https://github.com/dev-Aatif/Hayya/releases/download/v1.0.1/Hayya-v1.0.1.apk.sha256"
        val apkUrl = "https://github.com/dev-Aatif/Hayya/releases/download/v1.0.1/Hayya-v1.0.1.apk"

        val validEngine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(
                textResponses = mapOf(shaUrl to "$digestHex  Hayya-v1.0.1.apk"),
                downloadPayload = apkBytes
            ),
            currentVersionProvider = { "1.0.0" }
        )

        val release = AppReleaseInfo(
            tagName = "v1.0.1",
            versionName = "1.0.1",
            title = "Hayya v1.0.1",
            releaseNotes = "",
            htmlUrl = "https://github.com/dev-Aatif/Hayya/releases/tag/v1.0.1",
            apkDownloadUrl = apkUrl,
            apkAssetName = "Hayya-v1.0.1.apk",
            apkSizeBytes = apkBytes.size.toLong(),
            sha256AssetUrl = shaUrl
        )

        val progressUpdates = mutableListOf<Int>()
        val ok = validEngine.downloadAndVerifyApk(release) { progressUpdates.add(it) }
        assertThat(ok.isSuccess).isTrue()
        assertThat(ok.getOrThrow().exists()).isTrue()
        assertThat(progressUpdates).contains(100)

        // Now verify mismatch deletes the downloaded APK and fails
        val wrongHashEngine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(
                textResponses = mapOf(shaUrl to "${"0".repeat(64)}  Hayya-v1.0.1.apk"),
                downloadPayload = apkBytes
            ),
            currentVersionProvider = { "1.0.0" }
        )

        val mismatch = wrongHashEngine.downloadAndVerifyApk(release) {}
        assertThat(mismatch.isFailure).isTrue()
        assertThat(File(cacheDir, "updates/Hayya-v1.0.1.apk").exists()).isFalse()

        // Verify missing/failed SHA-256 fetch fails closed and deletes downloaded APK
        val missingShaEngine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(
                textResponses = emptyMap(),
                downloadPayload = apkBytes
            ),
            currentVersionProvider = { "1.0.0" }
        )

        val shaFetchFailure = missingShaEngine.downloadAndVerifyApk(release) {}
        assertThat(shaFetchFailure.isFailure).isTrue()
        assertThat(File(cacheDir, "updates/Hayya-v1.0.1.apk").exists()).isFalse()

        // Verify malformed SHA-256 file fails closed and deletes downloaded APK
        val malformedShaEngine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(
                textResponses = mapOf(shaUrl to "not-a-valid-sha256-digest"),
                downloadPayload = apkBytes
            ),
            currentVersionProvider = { "1.0.0" }
        )

        val malformedFailure = malformedShaEngine.downloadAndVerifyApk(release) {}
        assertThat(malformedFailure.isFailure).isTrue()
        assertThat(File(cacheDir, "updates/Hayya-v1.0.1.apk").exists()).isFalse()
    }

    @Test
    fun downloadAndVerifyApk_failsClosed_whenSha256AssetUrlIsMissing() = runTest {
        val apkBytes = "fake-apk-binary-payload".toByteArray()
        val releaseWithoutSha = AppReleaseInfo(
            tagName = "v1.1.0",
            versionName = "1.1.0",
            title = "Hayya v1.1.0",
            releaseNotes = "",
            htmlUrl = "https://github.com/dev-Aatif/Hayya/releases/tag/v1.1.0",
            apkDownloadUrl = "https://github.com/dev-Aatif/Hayya/releases/download/v1.1.0/Hayya-v1.1.0.apk",
            apkAssetName = "Hayya-v1.1.0.apk",
            apkSizeBytes = apkBytes.size.toLong(),
            sha256AssetUrl = null
        )
        val engine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(downloadPayload = apkBytes),
            currentVersionProvider = { "1.0.0" }
        )

        val result = engine.downloadAndVerifyApk(releaseWithoutSha) {}
        assertThat(result.isFailure).isTrue()
        assertThat(File(cacheDir, "updates/Hayya-v1.1.0.apk").exists()).isFalse()
    }

    @Test
    fun downloadAndVerifyApk_rejectsUntrustedHosts() = runTest {
        val evilRelease = AppReleaseInfo(
            tagName = "v1.1.0",
            versionName = "1.1.0",
            title = "Hayya v1.1.0",
            releaseNotes = "",
            htmlUrl = "https://github.com/dev-Aatif/Hayya/releases/tag/v1.1.0",
            apkDownloadUrl = "https://evil.example.com/Hayya-v1.1.0.apk",
            apkAssetName = "Hayya-v1.1.0.apk",
            apkSizeBytes = 128L,
            sha256AssetUrl = "https://github.com/dev-Aatif/Hayya/releases/download/v1.1.0/Hayya-v1.1.0.apk.sha256"
        )
        val engine = AppUpdateEngine(
            context = context,
            httpTransport = FakeTransport(downloadPayload = byteArrayOf(1, 2, 3)),
            currentVersionProvider = { "1.0.0" }
        )

        assertThat(engine.downloadAndVerifyApk(evilRelease) {}.isFailure).isTrue()
        assertThat(AppUpdateEngine.isAllowedUpdateUrl("https://api.github.com/repos/dev-Aatif/Hayya/releases/latest")).isTrue()
        assertThat(AppUpdateEngine.isAllowedUpdateUrl("https://objects.githubusercontent.com/github-production-release-asset/foo.apk")).isTrue()
        assertThat(AppUpdateEngine.isAllowedUpdateUrl("http://github.com/dev-Aatif/Hayya")).isFalse()
        assertThat(AppUpdateEngine.isAllowedUpdateUrl("https://github.com@evil.com/Hayya.apk")).isFalse()
        assertThat(AppUpdateEngine.isAllowedUpdateUrl("https://evil.com/Hayya.apk")).isFalse()
    }

    @Test
    fun sanitizeApkFileName_preventsPathTraversal() {
        assertThat(AppUpdateEngine.sanitizeApkFileName("../../evil.apk")).isEqualTo("evil.apk")
        assertThat(AppUpdateEngine.sanitizeApkFileName("Hayya-v1.0.1.apk")).isEqualTo("Hayya-v1.0.1.apk")
        assertThat(AppUpdateEngine.sanitizeApkFileName("bad/path/update")).isEqualTo("update.apk")
    }

    private class FakeTransport(
        private val textResponses: Map<String, String> = emptyMap(),
        private val textError: Throwable? = null,
        private val downloadPayload: ByteArray = ByteArray(0)
    ) : UpdateHttpTransport {

        override suspend fun getText(url: String): Result<String> {
            if (textError != null) return Result.failure(textError)
            val body = textResponses[url] ?: return Result.failure(IllegalStateException("404: $url"))
            return Result.success(body)
        }

        override suspend fun downloadToFile(
            url: String,
            destinationFile: File,
            onProgress: (Int) -> Unit
        ): Result<Unit> {
            destinationFile.parentFile?.mkdirs()
            destinationFile.writeBytes(downloadPayload)
            onProgress(50)
            onProgress(100)
            return Result.success(Unit)
        }
    }

    private companion object {
        const val LATEST_URL = "https://api.github.com/repos/dev-Aatif/Hayya/releases/latest"
    }
}
