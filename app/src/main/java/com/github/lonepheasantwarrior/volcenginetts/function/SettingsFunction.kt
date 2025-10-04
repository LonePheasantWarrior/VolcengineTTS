package com.github.lonepheasantwarrior.volcenginetts.function

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 设置相关功能
 */
class SettingsFunction(private val context: Context) {
    // SharedPreferences文件名
    private val prefsName = "VolcengineTTS_prefs"
    
    // SharedPreferences键名
    private val appId = "app_id"
    private val token = "token"
    private val speakerId = "selected_speaker_id"
    
    /**
     * 获取SharedPreferences实例
     */
    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存设置信息
     * @param appId 应用ID
     * @param token 令牌
     * @param selectedSpeakerId 选中的声音ID
     */
    fun saveSettings(appId: String, token: String, selectedSpeakerId: String) {
        getPreferences().edit {
            putString(this@SettingsFunction.appId, appId)
            putString(this@SettingsFunction.token, token)
            putString(speakerId, selectedSpeakerId)
        }
    }
    
    /**
     * 获取设置信息
     * @return 包含appId、token和selectedSpeakerId的三元组
     */
    fun getSettings(): Triple<String, String, String> {
        val prefs = getPreferences()
        val appId = prefs.getString(appId, "") ?: ""
        val token = prefs.getString(token, "") ?: ""
        val selectedSpeakerId = prefs.getString(speakerId, "") ?: ""
        return Triple(appId, token, selectedSpeakerId)
    }
}