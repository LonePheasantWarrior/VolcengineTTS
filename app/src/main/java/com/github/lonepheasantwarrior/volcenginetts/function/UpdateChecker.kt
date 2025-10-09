package com.github.lonepheasantwarrior.volcenginetts.function

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 检查应用更新的工具类
 */
class UpdateChecker(private val context: Context) {
    companion object {
        private const val UPDATE_API_URL = "https://gitee.com/api/v5/repos/LonePheasantWarrior/volcengine-tts/releases/latest"
        private const val RELEASE_PAGE_URL = "https://gitee.com/LonePheasantWarrior/volcengine-tts/releases/latest"
    }

    /**
     * 检查是否有新版本
     */
    fun checkForUpdates(callback: (Boolean, String?, String?, String?) -> Unit) {
        // 获取当前应用版本
        val currentVersion = getCurrentVersion()
        
        // 发起网络请求检查更新
        CoroutineScope(Dispatchers.IO).launch {
            fetchLatestReleaseInfo { success, latestVersion, downloadUrl, errorMsg ->
                if (success && latestVersion != null) {
                    // 移除版本号前的"v"字符进行比较
                    val cleanLatestVersion = latestVersion.removePrefix("v")
                    val hasUpdate = compareVersions(cleanLatestVersion, currentVersion)
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(hasUpdate, latestVersion, downloadUrl, null)
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(false, null, null, errorMsg ?: "检查更新失败")
                    }
                }
            }
        }
    }

    /**
     * 获取当前应用版本号
     */
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(LogTag.ERROR, "获取应用版本失败", e)
            ""
        }
    }

    /**
     * 比较版本号
     * @param latestVersion 最新版本号
     * @param currentVersion 当前版本号
     * @return 是否有新版本
     */
    private fun compareVersions(latestVersion: String, currentVersion: String): Boolean {
        try {
            val latestParts = latestVersion.split(".").map { it.toInt() }
            val currentParts = currentVersion.split(".").map { it.toInt() }
            
            for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                val latest = if (i < latestParts.size) latestParts[i] else 0
                val current = if (i < currentParts.size) currentParts[i] else 0
                
                if (latest > current) return true
                if (latest < current) return false
            }
            
            return false // 版本相同
        } catch (e: Exception) {
            Log.e(LogTag.ERROR, "版本号比较失败: latest=$latestVersion, current=$currentVersion", e)
            return false
        }
    }

    /**
     * 获取最新版本信息
     */
    private fun fetchLatestReleaseInfo(callback: (Boolean, String?, String?, String?) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder()
            .url(UPDATE_API_URL)
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(LogTag.ERROR, "检查更新请求失败", e)
                callback(false, null, null, "网络请求失败: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(LogTag.ERROR, "检查更新请求失败，响应码: ${response.code}")
                    callback(false, null, null, "服务器响应错误: ${response.code}")
                    return
                }
                
                try {
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        callback(false, null, null, "服务器返回空数据")
                        return
                    }
                    
                    val jsonObject = JSONObject(responseBody)
                    val tagName = jsonObject.optString("tag_name")
                    val assetsArray = jsonObject.optJSONArray("assets")
                    
                    if (tagName.isEmpty()) {
                        callback(false, null, null, "无法解析版本信息")
                        return
                    }
                    
                    var downloadUrl: String? = null
                    if (assetsArray != null && assetsArray.length() > 0) {
                        val assetObject = assetsArray.getJSONObject(0)
                        downloadUrl = assetObject.optString("browser_download_url")
                    }
                    
                    callback(true, tagName, downloadUrl, null)
                } catch (e: Exception) {
                    Log.e(LogTag.ERROR, "解析更新信息失败", e)
                    callback(false, null, null, "数据解析失败: ${e.message}")
                }
            }
        })
    }

    /**
     * 获取发布页面URL
     */
    fun getReleasePageUrl(): String {
        return RELEASE_PAGE_URL
    }
}