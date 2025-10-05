package com.github.lonepheasantwarrior.volcenginetts.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.github.lonepheasantwarrior.volcenginetts.TTSApplication
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 语音合成引擎回调监听服务
 */
class SynthesisEngineListener(private val context: Context): com.bytedance.speech.speechengine.SpeechEngine.SpeechListener {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val audioDataQueue: BlockingQueue<ByteArray?> get() = (context as TTSApplication).audioDataQueue
    private val isAudioQueueDone: AtomicBoolean get() = (context as TTSApplication).isAudioQueueDone

    override fun onSpeechMessage(type: Int, data: ByteArray?, len: Int) {
        var stdData = ""
        if (data != null && data.isNotEmpty()) {
            stdData = String(data)
        }

        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                Log.d(LogTag.SDK_INFO, "引擎启动通知: $stdData")
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> {
                Log.d(LogTag.SDK_INFO, "引擎关闭通知: $stdData")
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                Log.e(LogTag.SDK_ERROR, "引擎错误通知: $stdData")
                isAudioQueueDone.set(true)
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
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS -> {
                Log.d(LogTag.SDK_INFO, "引擎语音播放进度通知: $stdData")
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_FINISH_PLAYING -> {
                Log.d(LogTag.SDK_INFO, "引擎语音播放结束通知: $stdData")
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA -> {
                var dataSize = 0
                if (data != null) {
                    dataSize = data.size
                }
                Log.d(LogTag.SDK_INFO, "引擎音频数据通知, 数据大小: $dataSize")
                audioDataQueue.put(data)
                isAudioQueueDone.set(false)
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA_END -> {
                var dataSize = 0
                if (data != null) {
                    dataSize = data.size
                }
                Log.d(LogTag.SDK_INFO, "引擎音频数据(END)通知, 数据大小: $dataSize")
                audioDataQueue.put(data)
                isAudioQueueDone.set(true)
            }

            else -> {
                Log.d(LogTag.SDK_INFO, "引擎通知($type): $stdData")
            }
        }
    }
}