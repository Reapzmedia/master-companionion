package com.mastercompanion.platform.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootShell @Inject constructor() {

    @Volatile
    private var isRootAvailableCache: Boolean? = null

    /**
     * Checks if the su binary is available and root permissions can be acquired.
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        isRootAvailableCache?.let { return@withContext it }
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exitCode = process.waitFor()
            val available = exitCode == 0 && output.contains("uid=0(root)")
            isRootAvailableCache = available
            Timber.i("Root check result: $available (output: $output)")
            available
        } catch (e: Exception) {
            Timber.w(e, "Root access check failed or denied")
            isRootAvailableCache = false
            false
        }
    }

    /**
     * Executes a command with root privileges and returns stdout string.
     */
    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(stdout)
            } else {
                val err = if (stderr.isNotBlank()) stderr else "Command exited with code $exitCode"
                Timber.w("Root command error: $command -> $err")
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed executing root command: $command")
            Result.failure(e)
        }
    }

    /**
     * Reads the contents of a sysfs file using root shell.
     */
    suspend fun readSysfs(path: String): Result<String> {
        return execute("cat $path 2>/dev/null")
    }

    /**
     * Writes a value into a sysfs file using root shell with DAC permission elevation.
     */
    suspend fun writeSysfs(path: String, value: String): Result<Unit> {
        return execute("echo '$value' > $path 2>/dev/null || (chmod 666 $path 2>/dev/null && echo '$value' > $path)").map { }
    }

    /**
     * Checks if a sysfs or Linux kernel file exists and is accessible.
     */
    suspend fun checkFileExists(path: String): Boolean {
        return execute("[ -e \"$path\" ] && echo 1 || echo 0").getOrNull()?.trim() == "1"
    }
}
