package com.github.lonepheasantwarrior.volcenginetts.function

import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.lonepheasantwarrior.volcenginetts.R
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

/**
 * 更新检查相关功能
 */
class UpdateFunction(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * 检查是否有新版本
     * @param currentVersion 当前版本号
     * @return UpdateResult 包含更新信息和下载链接
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateResult {
        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(context.getString(R.string.update_check_url))
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext UpdateResult.Error("网络请求失败: ${response.code}")
                    }
                    
                    val responseBody = response.body.string()
                    if (responseBody.isEmpty()) {
                        return@withContext UpdateResult.Error("响应内容为空")
                    }
                    
                    val json = JSONObject(responseBody)
                    val latestVersion = json.getString("tag_name").removePrefix("v")
                    val releaseNotes = json.optString("body", "")
                    
                    Log.d(LogTag.INFO, "当前版本: $currentVersion, 最新版本: $latestVersion")
                    
                    // 比较版本号
                    if (isNewVersionAvailable(currentVersion, latestVersion)) {
                        val downloadUrl = findApkDownloadUrl(json)
                        if (downloadUrl.isNotEmpty()) {
                            UpdateResult.UpdateAvailable(latestVersion, releaseNotes, downloadUrl)
                        } else {
                            UpdateResult.Error("未找到APK下载链接")
                        }
                    } else {
                        UpdateResult.NoUpdate
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(LogTag.ERROR, "检查更新失败", e)
            UpdateResult.Error("检查更新失败: ${e.message}")
        }
    }
    
    /**
     * 比较版本号，判断是否有新版本
     */
    private fun isNewVersionAvailable(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        
        // 比较主版本号、次版本号、修订号
        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            
            if (latestPart > currentPart) {
                return true
            } else if (latestPart < currentPart) {
                return false
            }
        }
        
        return false
    }
    
    /**
     * 从JSON中查找APK下载链接
     */
    private fun findApkDownloadUrl(json: JSONObject): String {
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.contains("apk", ignoreCase = true)) {
                    return asset.getString("browser_download_url")
                }
            }
        }
        return ""
    }
    
    /**
     * 打开浏览器下载APK
     */
    fun downloadApk(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, downloadUrl.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(LogTag.ERROR, "打开下载链接失败", e)
        }
    }
}

/**
 * 更新检查结果
 */
class UpdateResult private constructor() {
    companion object {
        val NoUpdate = UpdateResult()
        
        fun UpdateAvailable(version: String, releaseNotes: String, downloadUrl: String): UpdateResult {
            return UpdateResult().apply {
                this.version = version
                this.releaseNotes = releaseNotes
                this.downloadUrl = downloadUrl
                this.type = ResultType.UPDATE_AVAILABLE
            }
        }
        
        fun Error(message: String): UpdateResult {
            return UpdateResult().apply {
                this.message = message
                this.type = ResultType.ERROR
            }
        }
    }
    
    private enum class ResultType {
        NO_UPDATE, UPDATE_AVAILABLE, ERROR
    }
    
    private var type: ResultType = ResultType.NO_UPDATE
    var version: String = ""
    var releaseNotes: String = ""
    var downloadUrl: String = ""
    var message: String = ""
    
    fun isUpdateAvailable(): Boolean = type == ResultType.UPDATE_AVAILABLE
    fun isError(): Boolean = type == ResultType.ERROR
    fun isNoUpdate(): Boolean = type == ResultType.NO_UPDATE
}