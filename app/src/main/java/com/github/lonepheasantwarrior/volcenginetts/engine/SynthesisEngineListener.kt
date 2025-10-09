package com.github.lonepheasantwarrior.volcenginetts.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.github.lonepheasantwarrior.volcenginetts.TTSApplication
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import com.github.lonepheasantwarrior.volcenginetts.tts.TTSContext

/**
 * 语音合成引擎回调监听服务
 */
class SynthesisEngineListener(private val context: Context): com.bytedance.speech.speechengine.SpeechEngine.SpeechListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val synthesisEngine: SynthesisEngine get() = (context as TTSApplication).synthesisEngine
    private val ttsContext: TTSContext get() = (context as TTSApplication).ttsContext
    
    // 定义一个特殊的空字节数组，用于表示控制信号而不是实际的音频数据
    private val controlSignal = ByteArray(0)

    override fun onSpeechMessage(type: Int, data: ByteArray?, len: Int) {
        ttsContext.currentEngineState.set(type)
        var stdData = ""
        if (data != null && data.isNotEmpty()) {
            stdData = String(data)
        }

        if (ttsContext.isTTSInterrupted.get()) {
            Log.w(LogTag.INFO, "收到语音合成作业中断信号, 忽略合成引擎回调: $stdData")
            return
        }

        ttsContext.currentEngineMsg.set(stdData)

        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                Log.d(LogTag.SDK_INFO, "引擎启动通知: $stdData")
                val speechEngine = synthesisEngine.getEngine()
                if (speechEngine == null) {
                    Log.e(LogTag.SDK_ERROR, "未能获取到语音合成引擎实例")
                    mainHandler.post {
                        Toast.makeText(context, "未能获取到语音合成引擎实例", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                //连续合成场景下，使用该指令触发一次合成，可以多次调用。推荐的调用策略如下：
                //第一句文本，直接调用；
                //非首句文本，在收到 合成结束回调 后发送；
                //使用 SDK 内置播放器时，如果返回值为 ERR_SYNTHESIS_PLAYER_IS_BUSY，表明内部缓存已经耗尽，应该在收到下一个 播放结回调时 再次调用；
                //“合成”指令必须要在收到 MESSAGE_TYPE_ENGINE_START 后发送
                speechEngine.sendDirective(SpeechEngineDefines.DIRECTIVE_SYNTHESIS, "")
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                Log.d(LogTag.SDK_INFO, "引擎关闭通知: $stdData")
                ttsContext.isAudioQueueDone.set(true)
                ttsContext.audioDataQueue.put(controlSignal)
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                ttsContext.isAudioQueueDone.set(true)
                ttsContext.isTTSEngineError.set(true)
                ttsContext.audioDataQueue.put(controlSignal)
                Log.e(LogTag.SDK_ERROR, "引擎错误通知: $stdData")
                mainHandler.post {
                    Toast.makeText(context, "引擎错误: $stdData", Toast.LENGTH_SHORT).show()
                }
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_BEGIN -> {
                Log.d(LogTag.SDK_INFO, "引擎语音合成开始通知: $stdData")
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_END -> {
                Log.d(LogTag.SDK_INFO, "引擎语音合成结束通知: $stdData")
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_START_PLAYING -> {
                Log.d(LogTag.SDK_INFO, "引擎语音播放开始通知: $stdData")
                ttsContext.isAudioQueueDone.set(false)
                ttsContext.audioDataQueue.put(controlSignal)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS -> {
                Log.d(LogTag.SDK_INFO, "引擎语音播放进度通知: $stdData")
                ttsContext.isAudioQueueDone.set(false)
                ttsContext.audioDataQueue.put(controlSignal)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_FINISH_PLAYING -> {
                Log.d(LogTag.SDK_INFO, "引擎语音播放结束通知: $stdData")
                ttsContext.isAudioQueueDone.set(true)
                ttsContext.audioDataQueue.put(controlSignal)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA -> {
                var dataSize = 0
                if (data != null) {
                    dataSize = data.size
                }
                Log.d(LogTag.SDK_INFO, "引擎音频数据通知, 数据大小: $dataSize")
                ttsContext.isAudioQueueDone.set(false)
                ttsContext.audioDataQueue.put(data)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA_END -> {
                var dataSize = 0
                if (data != null) {
                    dataSize = data.size
                }
                Log.d(LogTag.SDK_INFO, "引擎音频数据(END)通知, 数据大小: $dataSize")
                ttsContext.isAudioQueueDone.set(true)
                ttsContext.audioDataQueue.put(data)
            }

            else -> {
                Log.d(LogTag.SDK_INFO, "引擎通知($type): $stdData")
            }
        }
    }
}