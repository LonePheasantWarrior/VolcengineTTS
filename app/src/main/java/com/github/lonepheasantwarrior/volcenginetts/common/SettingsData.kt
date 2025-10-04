package com.github.lonepheasantwarrior.volcenginetts.common

/**
 * 设置数据类
 * @param appId 应用ID
 * @param token 令牌
 * @param selectedSpeakerId 选中的声音ID
 * @param serviceCluster 接口服务簇
 */
data class SettingsData(
    val appId: String,
    val token: String,
    val selectedSpeakerId: String,
    val serviceCluster: String
)