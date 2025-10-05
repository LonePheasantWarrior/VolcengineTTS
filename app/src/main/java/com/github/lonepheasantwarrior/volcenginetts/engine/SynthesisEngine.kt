package com.github.lonepheasantwarrior.volcenginetts.engine

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.github.lonepheasantwarrior.volcenginetts.R
import com.github.lonepheasantwarrior.volcenginetts.TTSApplication
import com.github.lonepheasantwarrior.volcenginetts.common.Constants
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * 语音合成引擎
 */
class SynthesisEngine(private val context: Context) {
    private var mSpeechEngine: SpeechEngine? = null
    private var isInitialized: Boolean = false
    private var isParametersBeenSet: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val synthesisEngineListener: SynthesisEngineListener get() = (context as TTSApplication).synthesisEngineListener

    /**
     * 初始化语音合成引擎
     */
    fun create(
        appId: String,
        token: String,
        speakerId: String,
        serviceCluster: String,
        isEmotional: Boolean
    ): SpeechEngine {
        if (mSpeechEngine != null) {
            destroy()
        }
        mSpeechEngine = SpeechEngineGenerator.getInstance()
        mSpeechEngine!!.createEngine()
        Log.d(LogTag.SDK_INFO, "语音合成SDK版本号: " + mSpeechEngine!!.version)
        // 初始化引擎配置
        setEngineParams(appId, token, speakerId, serviceCluster, isEmotional)
        return mSpeechEngine!!
    }

    /**
     * 初始化语音合成引擎相关配置
     */
    private fun setEngineParams(
        appId: String,
        token: String,
        speakerId: String,
        serviceCluster: String,
        isEmotional: Boolean
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
        //语音合成服务所用服务簇ID
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
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_ONLINE_STRING, Constants.VOICE
        )
        //是否使用SDK内置播放器播放合成出的音频
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_PLAYER_BOOL, false
        )
        //是否启用在线合成的情感预测功能
        mSpeechEngine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_WITH_INTENT_BOOL,
            isEmotional
        )
        //User ID（用以辅助定位线上用户问题）
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_UID_STRING,
            generateMD5(token)
        )
        //Device ID（用以辅助定位线上用户问题）
        mSpeechEngine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_DEVICE_ID_STRING,
            getDeviceId()
        )

        isInitialized = true
    }

    /**
     * 启动引擎
     */
    private fun startEngine(
        text: CharSequence?,
        speedRatio: Int?,
        volumeRatio: Int?,
        pitchRatio: Int?
    ) {
        // Directive：启动引擎前调用SYNC_STOP指令，保证前一次请求结束。
        var ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
        if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
            Log.e(LogTag.SDK_ERROR, "历史引擎关闭失败: $ret")
            mainHandler.post {
                Toast.makeText(context, "历史引擎关闭失败: $ret", Toast.LENGTH_SHORT).show()
            }
        } else {
            setTTSParams(text, speedRatio, volumeRatio, pitchRatio)
            ret = mSpeechEngine!!.initEngine()
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(LogTag.SDK_ERROR, "引擎初始化失败: $ret")
                mainHandler.post {
                    Toast.makeText(context, "引擎初始化失败: $ret", Toast.LENGTH_SHORT).show()
                }
            }
            mSpeechEngine!!.setListener(synthesisEngineListener)
            ret = mSpeechEngine!!.sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
            if (ret != SpeechEngineDefines.ERR_NO_ERROR) {
                Log.e(LogTag.SDK_ERROR, "引擎启动失败: $ret")
                mainHandler.post {
                    Toast.makeText(context, "引擎启动失败: $ret", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 设置语音合成参数
     */
    fun setTTSParams(
        text: CharSequence?,
        speedRatio: Int?,
        volumeRatio: Int?,
        pitchRatio: Int?
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


        //用于控制 TTS 音频的语速，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        if (speedRatio != null) {
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_TTS_SPEED_INT,
                speedRatio
            )
        }
        //用于控制 TTS 音频的音量，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        if (volumeRatio != null) {
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_TTS_VOLUME_INT,
                volumeRatio
            )
        }
        //用于控制 TTS 音频的音高，支持的配置范围参考火山官网 语音技术/语音合成/离在线语音合成SDK/参数说明 文档
        if (pitchRatio != null) {
            mSpeechEngine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_TTS_PITCH_INT,
                pitchRatio
            )
        }

        isParametersBeenSet = true
    }

    /**
     * 是否已创建
     */
    fun isCreated(): Boolean {
        return isInitialized
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
        isParametersBeenSet = false
    }

    /**
     * 获取设备ID
     */
    private fun getDeviceId(): String {
        // 使用设备硬件信息组合生成设备ID
        val sb = StringBuilder()
        sb.append(Build.BOARD).append("/")
        sb.append(Build.BRAND).append("/")
        sb.append(Build.DEVICE).append("/")
        sb.append(Build.HARDWARE).append("/")
        sb.append(Build.MODEL).append("/")
        sb.append(Build.PRODUCT).append("/")
        sb.append(Build.TAGS).append("/")
        sb.append(Build.TYPE).append("/")
        sb.append(Build.USER)

        return generateMD5(sb.toString())
    }

    /**
     * 生成字符串的MD5摘要
     */
    private fun generateMD5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val messageDigest = md.digest(input.toByteArray())
            val hexString = StringBuilder()
            for (b in messageDigest) {
                val hex = Integer.toHexString(0xFF and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            Log.e(LogTag.ERROR, "MD5 algorithm not available", e)
            ""
        }
    }
}
