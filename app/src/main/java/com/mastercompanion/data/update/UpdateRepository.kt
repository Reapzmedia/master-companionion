package com.mastercompanion.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mastercompanion.BuildConfig
import com.mastercompanion.di.IoDispatcher
import com.mastercompanion.platform.root.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val rootShell: RootShell,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val releasesApiUrl = "https://api.github.com/repos/Reapzmedia/master-companionion/releases/latest"

    suspend fun checkForUpdate(): UpdateStatus = withContext(ioDispatcher) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val request = Request.Builder()
                .url(releasesApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MasterCompanion-Updater")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Failed to fetch releases: HTTP ${response.code}"
                Timber.w(err)
                val status = UpdateStatus.Error(err)
                _updateStatus.value = status
                return@withContext status
            }

            val responseBody = response.body?.string() ?: ""
            val release = json.decodeFromString<GitHubRelease>(responseBody)

            val remoteVersion = release.tag_name.trim().removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME.trim().removePrefix("v")

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

            val status = if (apkAsset != null && isNewerVersion(remoteVersion, currentVersion)) {
                Timber.i("Update found! Current: $currentVersion, New: $remoteVersion")
                UpdateStatus.UpdateAvailable(
                    newVersion = release.tag_name,
                    releaseName = release.name.ifBlank { "Version ${release.tag_name}" },
                    changelog = release.body,
                    downloadUrl = apkAsset.browser_download_url,
                    apkSize = apkAsset.size
                )
            } else {
                Timber.i("App is up to date (current: $currentVersion, remote: $remoteVersion)")
                UpdateStatus.UpToDate
            }

            _updateStatus.value = status
            status
        } catch (e: Exception) {
            Timber.e(e, "Error checking for updates from GitHub")
            val status = UpdateStatus.Error(e.localizedMessage ?: "Unknown network error")
            _updateStatus.value = status
            status
        }
    }

    suspend fun downloadAndInstall(downloadUrl: String): Result<File> = withContext(ioDispatcher) {
        _updateStatus.value = UpdateStatus.Downloading(0)
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "MasterCompanion-Updater")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Download failed: HTTP ${response.code}"
                _updateStatus.value = UpdateStatus.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val body = response.body ?: return@withContext Result.failure(IllegalStateException("Empty body"))
            val totalBytes = body.contentLength()

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "app-release.apk")
            if (apkFile.exists()) apkFile.delete()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                _updateStatus.value = UpdateStatus.Downloading(percent)
                            }
                        }
                    }
                }
            }

            Timber.i("APK downloaded successfully (${apkFile.length()} bytes) to ${apkFile.absolutePath}")
            _updateStatus.value = UpdateStatus.ReadyToInstall(apkFile.absolutePath)

            // Trigger Installation
            triggerInstallation(apkFile)

            Result.success(apkFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to download and install update")
            _updateStatus.value = UpdateStatus.Error(e.localizedMessage ?: "Download failed")
            Result.failure(e)
        }
    }

    private suspend fun triggerInstallation(apkFile: File) {
        // Option 1: Root Silent Installation
        if (rootShell.isRootAvailable()) {
            Timber.i("Attempting silent root installation of ${apkFile.absolutePath}...")
            val result = rootShell.execute("pm install -r \"${apkFile.absolutePath}\"")
            val output = result.getOrNull() ?: ""
            if (result.isSuccess && output.contains("Success", ignoreCase = true)) {
                Timber.i("Silent Root PM Install successful!")
                return
            } else {
                Timber.w("Root PM install returned: $output. Falling back to PackageInstaller.")
            }
        }

        // Option 2: Android Standard PackageInstaller Intent via FileProvider
        withContext(ioDispatcher) {
            try {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
            } catch (e: Exception) {
                Timber.e(e, "Error launching PackageInstaller intent")
            }
        }
    }

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }

    /**
     * Compares two semantic version strings (e.g. "1.0.1" vs "1.0.0").
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        try {
            val remoteParts = remote.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
