package com.github.lonepheasantwarrior.volcenginetts

import android.app.Application
import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngine
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngineListener
import com.github.lonepheasantwarrior.volcenginetts.function.SettingsFunction
import com.github.lonepheasantwarrior.volcenginetts.tts.TTSContext
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class TTSApplication: Application() {
    lateinit var synthesisEngine: SynthesisEngine private set
    lateinit var synthesisEngineListener: SynthesisEngineListener private set
    lateinit var settingsFunction: SettingsFunction private set

    val ttsContext: TTSContext = TTSContext()

    override fun onCreate() {
        super.onCreate()
        // 初始化火山引擎语音合成环境
        // 在Application创建时调用，确保整个应用生命周期内只执行一次
        SpeechEngineGenerator.PrepareEnvironment(applicationContext, this)
        Log.d(LogTag.SDK_INFO, "火山引擎语音合成环境初始化完成")

        synthesisEngine = SynthesisEngine(this)
        synthesisEngineListener = SynthesisEngineListener(this)
        settingsFunction = SettingsFunction(this)
    }
}