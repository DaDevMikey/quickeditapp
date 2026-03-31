package com.example.quickeditapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val body: String
)

object UpdateChecker {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private const val REPO_URL = "https://api.github.com/repos/DaDevMikey/quickeditapp/releases/latest"

    fun checkForUpdates(currentVersion: String, callback: (GitHubRelease?) -> Unit) {
        val request = Request.Builder()
            .url(REPO_URL)
            .header("User-Agent", "QuickEditApp")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(null)
                        return
                    }
                    val body = it.body?.string() ?: ""
                    try {
                        val release = json.decodeFromString<GitHubRelease>(body)
                        // Simple version comparison
                        if (release.tag_name.trim().lowercase().removePrefix("v") != 
                            currentVersion.trim().lowercase().removePrefix("v")) {
                            callback(release)
                        } else {
                            callback(null)
                        }
                    } catch (e: Exception) {
                        callback(null)
                    }
                }
            }
        })
    }
}
