package com.github.lonepheasantwarrior.volcenginetts.core

import android.app.Application
import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag

class TTSApplication: Application() {
    lateinit var synthesisEngine: SynthesisEngine private set
    lateinit var synthesisEngineListener: SynthesisEngineListener private set

    override fun onCreate() {
        super.onCreate()
        // 初始化火山引擎语音合成环境
        // 在Application创建时调用，确保整个应用生命周期内只执行一次
        SpeechEngineGenerator.PrepareEnvironment(applicationContext, this)
        Log.d(LogTag.SDK_INFO, "火山引擎语音合成环境初始化完成")

        synthesisEngine = SynthesisEngine(this)
        synthesisEngineListener = SynthesisEngineListener(this)
    }
}