package com.github.lonepheasantwarrior.volcenginetts.function

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.lonepheasantwarrior.volcenginetts.common.SettingsData

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
    private val serviceCluster = "service_cluster"
    private val isEmotional = "is_emotional"
    
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
     * @param serviceCluster 接口服务簇
     * @param isEmotional 是否开启情感朗读
     */
    fun saveSettings(appId: String, token: String, selectedSpeakerId: String, serviceCluster: String, isEmotional: Boolean = false) {
        getPreferences().edit {
            putString(this@SettingsFunction.appId, appId)
            putString(this@SettingsFunction.token, token)
            putString(speakerId, selectedSpeakerId)
            putString(this@SettingsFunction.serviceCluster, serviceCluster)
            putBoolean(this@SettingsFunction.isEmotional, isEmotional)
        }
    }
    
    /**
     * 获取设置信息
     * @return 包含appId、token、selectedSpeakerId和serviceCluster的SettingsData对象
     */
    fun getSettings(): SettingsData {
        val prefs = getPreferences()
        val appId = prefs.getString(appId, "") ?: ""
        val token = prefs.getString(token, "") ?: ""
        val selectedSpeakerId = prefs.getString(speakerId, "") ?: ""
        val serviceCluster = prefs.getString(serviceCluster, "") ?: ""
        val isEmotional = prefs.getBoolean(isEmotional, false)
        return SettingsData(appId, token, selectedSpeakerId, serviceCluster, isEmotional)
    }
}