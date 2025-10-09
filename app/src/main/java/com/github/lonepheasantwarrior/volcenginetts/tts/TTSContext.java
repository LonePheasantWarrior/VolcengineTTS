package com.github.lonepheasantwarrior.volcenginetts.tts;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 语音合成作业上下文(全局唯一,要求单例)
 */
public class TTSContext {
    /**
     * 音频数据队列
     */
    public final BlockingQueue<byte[]> audioDataQueue = new LinkedBlockingQueue<>();
    /**
     * 音频队列是否装填完毕
     */
    public final AtomicBoolean isAudioQueueDone = new AtomicBoolean(true);
    /**
     * 是否已中断TTS作业
     */
    public final AtomicBoolean isTTSInterrupted = new AtomicBoolean(false);
    /**
     * 语音合成引擎是否发生错误
     */
    public final AtomicBoolean isTTSEngineError = new AtomicBoolean(false);
    /**
     * 当前语音合成引擎状态
     */
    public final AtomicInteger currentEngineState = new AtomicInteger();
    /**
     * 当前语音合成引擎信息
     */
    public final AtomicReference<String> currentEngineMsg = new AtomicReference<>();
}