package com.github.lonepheasantwarrior.volcenginetts.function

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag

/**
 * 应用更新管理器
 */
class UpdateManager(private val context: Context) {
    companion object {
        private const val RELEASE_PAGE_URL = "https://gitee.com/LonePheasantWarrior/volcengine-tts/releases/latest"
    }

    /**
     * 检查是否有新版本
     */
    fun checkForUpdates(onUpdateAvailable: (String, String?) -> Unit, onError: (String) -> Unit) {
        val updateChecker = UpdateChecker(context)
        updateChecker.checkForUpdates { hasUpdate, latestVersion, downloadUrl, errorMsg ->
            if (hasUpdate && latestVersion != null) {
                onUpdateAvailable(latestVersion, downloadUrl)
            } else if (errorMsg != null) {
                onError(errorMsg)
            }
        }
    }

    /**
     * 下载最新版本
     */
    fun downloadLatestVersion(downloadUrl: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, downloadUrl?.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(LogTag.ERROR, "打开下载链接失败", e)
            Toast.makeText(context, "打开下载链接失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 查看更新详情
     */
    fun viewUpdateDetails() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, RELEASE_PAGE_URL.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(LogTag.ERROR, "打开更新详情页失败", e)
            Toast.makeText(context, "打开更新详情页失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}