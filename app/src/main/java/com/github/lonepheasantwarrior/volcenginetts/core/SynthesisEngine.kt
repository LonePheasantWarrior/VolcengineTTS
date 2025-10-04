package com.github.lonepheasantwarrior.volcenginetts.core

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.github.lonepheasantwarrior.volcenginetts.R
import com.github.lonepheasantwarrior.volcenginetts.common.Dictionary
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag

/**
 * 语音合成引擎
 */
class SynthesisEngine {
    private var mSpeechEngine: SpeechEngine? = null
    private var isInitialized: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 初始化语音合成引擎
     */
    fun init(
        context: android.content.Context,
        appId: String,
        token: String,
        speakerId: String,
        serviceCluster: String,
        emotional: Boolean
    ) {
        if (mSpeechEngine != null) {
            destroy()
        }
        mSpeechEngine = SpeechEngineGenerator.getInstance()
        mSpeechEngine!!.createEngine()
        Log.d(LogTag.SDK_INFO, "语音合成SDK版本号: " + mSpeechEngine!!.version)
        // 初始化引擎配置
        initInner(context, appId, token, speakerId, serviceCluster, emotional)
    }

    /**
     * 初始化语音合成引擎相关配置
     */
    fun initInner(
        context: android.content.Context,
        appId: String,
        token: String,
        speakerId: String,
        serviceCluster: String,
        emotional: Boolean
    ) {
        //配置工作场景
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING, SpeechEngineDefines.TTS_ENGINE
        )
        //配置工作策略
        //TTS_WORK_MODE_ONLINE, 只进行在线合成
        //TTS_WORK_MODE_OFFLINE, 只进行离线合成
        //TTS_WORK_MODE_ALTERNATE, 先发起在线合成，失败后（网络超时），启动离线合成引擎开始合
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_WORK_MODE_INT,
            SpeechEngineDefines.TTS_WORK_MODE_ALTERNATE
        )
        //配置播放音源
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_AUDIO_STREAM_TYPE_INT,
            SpeechEngineDefines.AUDIO_STREAM_TYPE_MEDIA
        )
        //合成出的音频的采样率，默认为 24000
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_SAMPLE_RATE_INT,
            context.resources.getInteger(R.integer.tts_sample_rate)
        )
        //appId
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, appId)
        //token
        mSpeechEngine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, token)
        //语音合成服务簇
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_ADDRESS_STRING,
            context.getString(R.string.tts_service_address)
        )
        //语音合成服务接口
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_URI_STRING,
            context.getString(R.string.tts_service_api_path)
        )
        //语音合成服务所用集群
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_CLUSTER_STRING, serviceCluster
        )
        //是否返回音频数据
        mSpeechEngine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_DATA_CALLBACK_MODE_INT,
            SpeechEngineDefines.TTS_DATA_CALLBACK_MODE_ALL
        )
        //在线合成使用的音色代号
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_ONLINE_STRING, speakerId
        )
        //在线合成使用的“发音人类型”
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_ONLINE_STRING, Dictionary.SpeechEngine.VOICE
        )
        //是否使用SDK内置播放器播放合成出的音频
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_PLAYER_BOOL, false
        )
        //是否启用在线合成的情感预测功能
        mSpeechEngine!!.setOptionBoolean(SpeechEngineDefines.PARAMS_KEY_TTS_WITH_INTENT_BOOL, emotional)

        isInitialized = true
    }

    fun setTTSParams(
        context: android.content.Context,
        text: CharSequence?,
        speedRatio: Double,
        volumeRatio: Double,
        pitchRatio: Double
    ) {
        if (!isInitialized) {
            throw RuntimeException("语音合成引擎未初始化,无法执行合成参数配置操作")
        }
        if (text.isNullOrBlank()) {
            Log.e(LogTag.ERROR, "待合成文本为空")
            mainHandler.post {
                Toast.makeText(context, "待合成文本为空", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (text.length > 80) {
                mainHandler.post {
                    Toast.makeText(context, "单次合成文本不得超过80字", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //需合成的文本，不可超过 80 字
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_STRING, text as String
        )
        //TODO 功能待实现, 参考com.bytedance.speech.speechdemo.TtsNormalActivity#configStartTtsParams
    }

    /**
     * 销毁引擎
     */
    fun destroy() {
        if (mSpeechEngine != null) {
            mSpeechEngine!!.destroyEngine()
            mSpeechEngine = null
        }
        Log.i(LogTag.INFO, "引擎已销毁")
        isInitialized = false
    }
}
