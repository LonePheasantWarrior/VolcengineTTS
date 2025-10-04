package com.github.lonepheasantwarrior.volcenginetts.common

/**
 * 全局字典类
 */
class Dictionary {
    /**
     * 火山方舟-语音合成引擎相关
     */
    object SpeechEngine {
        /**
         * 发音人（非语音克隆场景使用other）
         */
        const val VOICE = "other"

        /**
         * 支持的语言列表（语言代码, 国家代码, 变体）
         * 例如: Pair("zh", "CN") 表示简体中文
         */
        val SUPPORTED_LANGUAGES = listOf(
            Triple("zh", "CN", null),    // 汉语（简体中文）
            Triple("zh", "TW", null),    // 汉语（繁体中文-台湾）
            Triple("zh", "HK", null),    // 汉语（繁体中文-香港）
        )
    }
}