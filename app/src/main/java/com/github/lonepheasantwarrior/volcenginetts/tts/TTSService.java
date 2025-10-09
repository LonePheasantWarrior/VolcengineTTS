package com.github.lonepheasantwarrior.volcenginetts.tts;

import android.media.AudioFormat;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.util.Log;
import android.widget.Toast;

import com.github.lonepheasantwarrior.volcenginetts.R;
import com.github.lonepheasantwarrior.volcenginetts.TTSApplication;
import com.github.lonepheasantwarrior.volcenginetts.common.Constants;
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag;
import com.github.lonepheasantwarrior.volcenginetts.common.SettingsData;
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngine;
import com.github.lonepheasantwarrior.volcenginetts.function.SettingsFunction;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TTSService extends TextToSpeechService {

    @Nullable
    private volatile String[] mCurrentLanguage = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SynthesisEngine synthesisEngine;
    private SettingsFunction settingsFunction;

    private TTSContext ttsContext;

    // 定义一个特殊的空字节数组，用于表示控制信号而不是实际的音频数据
    private static final byte[] CONTROL_SIGNAL = new byte[0];

    @Override
    public void onCreate() {
        super.onCreate();
        TTSApplication ttsApplication = ((TTSApplication) getApplicationContext());
        synthesisEngine = ttsApplication.getSynthesisEngine();
        settingsFunction = ttsApplication.getSettingsFunction();
        ttsContext = ttsApplication.getTtsContext();
    }

    @Override
    protected String[] onGetLanguage() {
        if (mCurrentLanguage == null) {
            // 默认使用简体中文(中国大陆)语言
            // 将语言代码转换为与Constants.supportedLanguages中定义的格式一致
            mCurrentLanguage = new String[]{"zho", "CHN", ""};
        }
        return mCurrentLanguage;
    }

    @Override
    protected int onIsLanguageAvailable(String lang, String country, String variant) {
        return getIsLanguageAvailable(lang, country, variant);
    }

    @Override
    protected int onLoadLanguage(String _lang, String _country, String _variant) {
        String lang = _lang == null ? "" : _lang;
        String country = _country == null ? "" : _country;
        String variant = _variant == null ? "" : _variant;
        int result = onIsLanguageAvailable(lang, country, variant);
        if (result == TextToSpeech.LANG_COUNTRY_AVAILABLE || TextToSpeech.LANG_AVAILABLE == result
                || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            mCurrentLanguage = new String[]{lang, country, variant};
        }
        return result;
    }

    @Override
    protected void onStop() {
        synthesisEngine.destroy();
    }

    @Override
    protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        long onSynthesizeStartTime = System.currentTimeMillis();

        SettingsData settings = settingsFunction.getSettings();
        if (!checkSettings(settings)) {
            callback.error();
            return;
        }

        if (request.getCharSequenceText() == null || request.getCharSequenceText().toString().isBlank()) {
            Log.d(LogTag.ERROR, "待合成文本为空");
            callback.start(16000,
                    AudioFormat.ENCODING_PCM_16BIT, 1 /* Number of channels. */);
            callback.done();
            return;
        }

        String text = request.getCharSequenceText().toString();
        Log.d(LogTag.INFO, "待合成文本长度: " + text.length());
        Log.d(LogTag.INFO, "待合成文本: " + text);

        // 如果文本长度超过80个字符，进行拆分处理
        if (text.length() > 80) {
            Log.d(LogTag.INFO, "文本长度超过80字符，开始拆分处理");
            synthesizeLongText(text, request, callback, settings);
        } else {
            synthesizeSingleText(text, request, callback, settings);
        }

        Log.d(LogTag.INFO, "语音合成任务执行完毕,耗时: " + (System.currentTimeMillis() - onSynthesizeStartTime) / 1000 + "秒");
    }

    public static int getIsLanguageAvailable(String lang, String country, String variant) {
        Locale locale = new Locale(lang, country, variant);
        boolean isLanguage = false;
        boolean isCountry = false;
        for (String lan : Constants.SUPPORTED_LANGUAGES) {
            String[] temp = lan.split("-");
            Locale locale1 = new Locale(temp[0], temp[1]);
            if (locale.getISO3Language().equals(locale1.getISO3Language())) {
                isLanguage = true;
            }
            if (isLanguage && locale.getISO3Country().equals(locale1.getISO3Country())) {
                isCountry = true;
            }
            if (isCountry && locale.getVariant().equals(locale1.getVariant())) {
                return TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
            }

        }
        if (isCountry) {
            return TextToSpeech.LANG_COUNTRY_AVAILABLE;
        }
        if (isLanguage) {
            return TextToSpeech.LANG_AVAILABLE;
        }
        return TextToSpeech.LANG_NOT_SUPPORTED;
    }

    /**
     * 检查配置是否有效
     *
     * @param settings 待检查配置
     * @return 检查结果
     */
    private boolean checkSettings(SettingsData settings) {
        if (settings == null) {
            mainHandler.post(() -> Toast.makeText(getApplicationContext(), "语音引擎未配置", Toast.LENGTH_SHORT).show());
            return false;
        }
        if (settings.getAppId().isBlank() || settings.getToken().isBlank()
                || settings.getServiceCluster().isBlank() || settings.getSelectedSpeakerId().isBlank()) {
            mainHandler.post(() -> Toast.makeText(getApplicationContext(), "语音引擎配置不可用", Toast.LENGTH_SHORT).show());
            return false;
        }
        return true;
    }

    /**
     * 智能拆分长文本为多个不超过80字符的片段
     * 优先按句子边界（句号、感叹号、问号等）拆分，其次按逗号，最后按空格
     *
     * @param text 原始文本
     * @return 拆分后的文本片段列表
     */
    private List<String> splitLongText(String text) {
        List<String> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        // 定义句子分隔符（中英文、全角半角）
        String[] sentenceDelimiters = {".", "。", "!", "！", "?", "？", ";", "；"};
        String[] clauseDelimiters = {",", "，"};

        int maxLength = 80;
        int currentPos = 0;

        while (currentPos < text.length()) {
            int remainingLength = text.length() - currentPos;

            // 如果剩余文本长度小于等于最大长度，直接添加
            if (remainingLength <= maxLength) {
                segments.add(text.substring(currentPos));
                break;
            }

            // 查找最佳拆分点
            int splitPos = findBestSplitPoint(text, currentPos, maxLength, sentenceDelimiters, clauseDelimiters);

            // 如果找不到合适的拆分点，按最大长度强制拆分
            if (splitPos == currentPos) {
                splitPos = currentPos + maxLength;
            }

            segments.add(text.substring(currentPos, splitPos).trim());
            currentPos = splitPos;
        }

        Log.d(LogTag.INFO, "文本拆分完成，共拆分为 " + segments.size() + " 个片段");
        return segments;
    }

    /**
     * 查找最佳拆分点
     *
     * @param text               文本
     * @param startPos           起始位置
     * @param maxLength          最大长度
     * @param sentenceDelimiters 句子分隔符
     * @param clauseDelimiters   从句分隔符
     * @return 最佳拆分位置
     */
    private int findBestSplitPoint(String text, int startPos, int maxLength,
                                   String[] sentenceDelimiters, String[] clauseDelimiters) {
        int endPos = Math.min(startPos + maxLength, text.length());

        // 优先查找句子结束位置
        for (String delimiter : sentenceDelimiters) {
            int pos = text.lastIndexOf(delimiter, endPos - 1);
            if (pos > startPos && pos - startPos <= maxLength) {
                return pos + delimiter.length(); // 包含分隔符
            }
        }

        // 其次查找从句分隔符
        for (String delimiter : clauseDelimiters) {
            int pos = text.lastIndexOf(delimiter, endPos - 1);
            if (pos > startPos && pos - startPos <= maxLength) {
                return pos + delimiter.length(); // 包含分隔符
            }
        }

        // 最后查找空格
        int spacePos = text.lastIndexOf(' ', endPos - 1);
        if (spacePos > startPos && spacePos - startPos <= maxLength) {
            return spacePos + 1; // 包含空格
        }

        // 如果都找不到，返回起始位置（表示需要强制拆分）
        return startPos;
    }

    /**
     * 合成单个文本片段（不超过80字符）
     *
     * @param text     待合成文本
     * @param request  原始请求
     * @param callback 合成回调
     * @param settings 配置信息
     */
    private void synthesizeSingleText(String text, SynthesisRequest request,
                                      SynthesisCallback callback, SettingsData settings) {
        synthesisEngine.create(settings.getAppId(), settings.getToken(),
                settings.getSelectedSpeakerId(), settings.getServiceCluster(), settings.isEmotional());
        synthesisEngine.startEngine(text, request.getSpeechRate(), null, request.getPitch());

        try {
            callback.start(getApplicationContext().getResources().getInteger(R.integer.tts_sample_rate)
                    , AudioFormat.ENCODING_PCM_16BIT, 1 /* Number of channels. */);

            Log.d(LogTag.INFO, "开始监听语音合成音频回调队列...");
            do {
                byte[] chunk = ttsContext.audioDataQueue.take();
                // 检查是否是控制信号（空字节数组）
                if (chunk != null && chunk != CONTROL_SIGNAL && chunk.length > 0) {
                    Log.d(LogTag.INFO, "向系统TTS服务提供音频Callback,数据长度: " + chunk.length);
                    int offset = 0;
                    while (offset < chunk.length) {
                        int chunkSize = Math.min(callback.getMaxBufferSize(), chunk.length - offset);
                        callback.audioAvailable(chunk, offset, chunkSize);
                        offset += chunkSize;
                    }
                }
                Log.d(LogTag.INFO, "音频队列是否消费完成: " + ttsContext.isAudioQueueDone.get());
            } while (!ttsContext.isAudioQueueDone.get());
            if (ttsContext.isTTSEngineError.get()) {
                throw new RuntimeException(ttsContext.currentEngineMsg.get());
            }
            callback.done();
        } catch (Exception e) {
            Log.e(LogTag.ERROR, "执行音频Callback发生错误: " + e.getMessage());
            callback.error();
        }
        synthesisEngine.destroy();
    }

    /**
     * 合成长文本（超过80字符）
     *
     * @param text     待合成文本
     * @param request  原始请求
     * @param callback 合成回调
     * @param settings 配置信息
     */
    private void synthesizeLongText(String text, SynthesisRequest request,
                                    SynthesisCallback callback, SettingsData settings) {
        List<String> segments = splitLongText(text);

        if (segments.isEmpty()) {
            callback.error();
            return;
        }

        // 初始化回调
        try {
            callback.start(getApplicationContext().getResources().getInteger(R.integer.tts_sample_rate)
                    , AudioFormat.ENCODING_PCM_16BIT, 1 /* Number of channels. */);
        } catch (Exception e) {
            Log.e(LogTag.ERROR, "初始化音频回调失败: " + e.getMessage());
            callback.error();
            return;
        }

        boolean hasError = false;

        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            Log.d(LogTag.INFO, "合成第 " + (i + 1) + "/" + segments.size() + " 个片段，长度: " + segment.length());
            Log.d(LogTag.INFO, "片段内容: " + segment);

            // 为每个片段创建新的合成引擎实例
            synthesisEngine.create(settings.getAppId(), settings.getToken(),
                    settings.getSelectedSpeakerId(), settings.getServiceCluster(), settings.isEmotional());
            synthesisEngine.startEngine(segment, request.getSpeechRate(), null, request.getPitch());

            try {
                // 处理当前片段的音频数据
                boolean segmentCompleted = false;
                ttsContext.isAudioQueueDone.set(false);
                ttsContext.isTTSEngineError.set(false);

                while (!segmentCompleted) {
                    byte[] chunk = ttsContext.audioDataQueue.take();
                    // 检查是否是控制信号（空字节数组）
                    if (chunk != null && chunk != CONTROL_SIGNAL && chunk.length > 0) {
                        Log.d(LogTag.INFO, "向系统TTS服务提供音频Callback,数据长度: " + chunk.length);
                        int offset = 0;
                        while (offset < chunk.length) {
                            int chunkSize = Math.min(callback.getMaxBufferSize(), chunk.length - offset);
                            callback.audioAvailable(chunk, offset, chunkSize);
                            offset += chunkSize;
                        }
                    }

                    // 检查当前片段是否完成
                    if (ttsContext.isAudioQueueDone.get()) {
                        segmentCompleted = true;
                        if (ttsContext.isTTSEngineError.get()) {
                            hasError = true;
                            Log.e(LogTag.ERROR, "第 " + (i + 1) + " 个片段合成失败");
                            break;
                        }
                    }
                }

                // 销毁当前片段的引擎
                synthesisEngine.destroy();

                // 如果发生错误，停止后续合成
                if (hasError) {
                    break;
                }

                // 在片段之间添加短暂停顿（除了最后一个片段）
                if (i < segments.size() - 1) {
                    addPauseBetweenSegments();
                }

            } catch (Exception e) {
                Log.e(LogTag.ERROR, "处理第 " + (i + 1) + " 个片段时发生错误: " + e.getMessage());
                hasError = true;
                synthesisEngine.destroy();
                break;
            }
        }

        if (hasError) {
            callback.error();
        } else {
            callback.done();
        }
    }

    /**
     * 在文本片段之间添加短暂停顿,尽可能避免下一轮播放时因为异步回调延迟导致播放期间收到引擎关闭通知进而带来意外停止
     */
    private void addPauseBetweenSegments() {
        try {
            Thread.sleep(300);
        } catch (Exception e) {
            Log.e(LogTag.ERROR, "PauseBetweenSegments错误: " + e.getMessage());
        }
    }
}
