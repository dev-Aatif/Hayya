package dev.hayya.core.domain.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateModelsTest {

    @Test
    fun versionComparator_comparesSemanticVersionsCorrectly() {
        assertThat(UpdateVersionComparator.isNewerVersion("v1.0.1", "1.0.0")).isTrue()
        assertThat(UpdateVersionComparator.isNewerVersion("1.1.0", "1.0.9")).isTrue()
        assertThat(UpdateVersionComparator.isNewerVersion("1.10.0", "1.2.0")).isTrue()
        assertThat(UpdateVersionComparator.isNewerVersion("v2.0.0", "1.9.9")).isTrue()

        assertThat(UpdateVersionComparator.isNewerVersion("v1.0.0", "1.0.0")).isFalse()
        assertThat(UpdateVersionComparator.isNewerVersion("1.0.0", "1.0.1")).isFalse()
        assertThat(UpdateVersionComparator.isNewerVersion("1.1.0-beta.1", "1.1.0")).isFalse()
        assertThat(UpdateVersionComparator.isNewerVersion("1.1.0", "1.1.0-beta.1")).isTrue()

        assertThat(UpdateVersionComparator.isNewerVersion("", "1.0.0")).isFalse()
        assertThat(UpdateVersionComparator.isNewerVersion("invalid", "1.0.0")).isFalse()
    }

    @Test
    fun gitHubReleaseParser_extractsReleaseAndApkAssets() {
        val json = """
            {
              "tag_name": "v1.0.1",
              "name": "Hayya v1.0.1",
              "draft": false,
              "prerelease": false,
              "published_at": "2026-11-26T00:00:00Z",
              "html_url": "https://github.com/dev-Aatif/Hayya/releases/tag/v1.0.1",
              "body": "Fixes and improvements:\n- \"Faster\" startup",
              "assets": [
                {
                  "name": "Hayya-v1.0.1.apk.sha256",
                  "size": 84,
                  "browser_download_url": "https://github.com/dev-Aatif/Hayya/releases/download/v1.0.1/Hayya-v1.0.1.apk.sha256"
                },
                {
                  "name": "Hayya-v1.0.1.apk",
                  "size": 9123456,
                  "browser_download_url": "https://github.com/dev-Aatif/Hayya/releases/download/v1.0.1/Hayya-v1.0.1.apk"
                }
              ]
            }
        """.trimIndent()

        val release = GitHubReleaseParser.parseLatestRelease(json)
        assertThat(release).isNotNull()
        assertThat(release!!.tagName).isEqualTo("v1.0.1")
        assertThat(release.versionName).isEqualTo("1.0.1")
        assertThat(release.title).isEqualTo("Hayya v1.0.1")
        assertThat(release.releaseNotes).isEqualTo("Fixes and improvements:\n- \"Faster\" startup")
        assertThat(release.apkAssetName).isEqualTo("Hayya-v1.0.1.apk")
        assertThat(release.apkSizeBytes).isEqualTo(9123456L)
        assertThat(release.apkDownloadUrl).isEqualTo("https://github.com/dev-Aatif/Hayya/releases/download/v1.0.1/Hayya-v1.0.1.apk")
        assertThat(release.sha256AssetUrl).isEqualTo("https://github.com/dev-Aatif/Hayya/releases/download/v1.0.1/Hayya-v1.0.1.apk.sha256")
    }

    @Test
    fun gitHubReleaseParser_ignoresDraftAndPrerelease() {
        val draftJson = """{"tag_name": "v1.0.2", "draft": true, "prerelease": false}"""
        val preJson = """{"tag_name": "v1.0.2-rc1", "draft": false, "prerelease": true}"""

        assertThat(GitHubReleaseParser.parseLatestRelease(draftJson)).isNull()
        assertThat(GitHubReleaseParser.parseLatestRelease(preJson)).isNull()
    }

    @Test
    fun parseSha256Hex_validatesSha256SumFormat() {
        val valid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef  Hayya-v1.0.1.apk\n"
        assertThat(GitHubReleaseParser.parseSha256Hex(valid))
            .isEqualTo("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")

        assertThat(GitHubReleaseParser.parseSha256Hex("not-a-hash")).isNull()
        assertThat(GitHubReleaseParser.parseSha256Hex("")).isNull()
    }
}
