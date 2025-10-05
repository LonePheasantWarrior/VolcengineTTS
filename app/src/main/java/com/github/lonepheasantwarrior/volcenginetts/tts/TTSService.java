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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TTSService extends TextToSpeechService {

    @Nullable
    private volatile String[] mCurrentLanguage = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SynthesisEngine synthesisEngine;
    private SettingsFunction settingsFunction;

    private TTSContext ttsContext;

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
        Log.d(LogTag.INFO, "待合成文本长度: " + request.getCharSequenceText().length());
        Log.d(LogTag.INFO, "待合成文本: " + request.getCharSequenceText());

        synthesisEngine.create(settings.getAppId(), settings.getToken(), settings.getSelectedSpeakerId(), settings.getServiceCluster(), settings.isEmotional());
        synthesisEngine.startEngine(request.getCharSequenceText(), request.getSpeechRate(), null, request.getPitch());

        try {
            callback.start(getApplicationContext().getResources().getInteger(R.integer.tts_sample_rate)
                    , AudioFormat.ENCODING_PCM_16BIT, 1 /* Number of channels. */);

            Log.d(LogTag.INFO, "开始监听语音合成音频回调队列...");
            long startTime = System.currentTimeMillis();
            while (true) {
                final long TIMEOUT_MS = 15000;
                if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                    Log.e(LogTag.ERROR, "监听语音合成音频回调超时,放弃本次合成任务");
                    break;
                }
                byte[] chunk = ttsContext.audioDataQueue.poll(100, TimeUnit.MILLISECONDS);
                if (chunk != null) {
                    Log.d(LogTag.INFO, "向系统TTS服务提供音频Callback,数据长度: " + chunk.length);
                    int offset = 0;
                    while (offset < chunk.length) {
                        int chunkSize = Math.min(callback.getMaxBufferSize(), chunk.length - offset);
                        callback.audioAvailable(chunk, offset, chunkSize);
                        offset += chunkSize;
                    }
                }
                if (ttsContext.isAudioQueueDone.get()) break;
            }
            if (ttsContext.isTTSEngineError.get()) {
                callback.error();
            }else {
                callback.done();
            }
        } catch (Exception e) {
            Log.e(LogTag.ERROR, "执行音频Callback发生错误: " + e.getMessage());
            callback.error();
        }
        synthesisEngine.destroy();
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
}
