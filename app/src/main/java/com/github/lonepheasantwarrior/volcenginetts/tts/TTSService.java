package com.github.lonepheasantwarrior.volcenginetts.tts;

import android.os.Handler;
import android.os.Looper;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.widget.Toast;

import com.github.lonepheasantwarrior.volcenginetts.TTSApplication;
import com.github.lonepheasantwarrior.volcenginetts.common.Constants;
import com.github.lonepheasantwarrior.volcenginetts.common.SettingsData;
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngine;
import com.github.lonepheasantwarrior.volcenginetts.function.SettingsFunction;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TTSService extends TextToSpeechService {

    @Nullable
    private volatile String[] mCurrentLanguage = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SynthesisEngine synthesisEngine;
    private SettingsFunction settingsFunction;

    private BlockingQueue<byte[]> audioDataQueue;
    private AtomicBoolean isAudioQueueDone;

    @Override
    public void onCreate() {
        super.onCreate();
        TTSApplication ttsApplication = ((TTSApplication) getApplicationContext());
        synthesisEngine = ttsApplication.getSynthesisEngine();
        settingsFunction = ttsApplication.getSettingsFunction();
        audioDataQueue = ttsApplication.getAudioDataQueue();
        isAudioQueueDone = ttsApplication.isAudioQueueDone();
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
        synthesisEngine.create(settings.getAppId(), settings.getToken(), settings.getSelectedSpeakerId(), settings.getServiceCluster(), settings.isEmotional());
        synthesisEngine.startEngine(request.getCharSequenceText(), request.getSpeechRate(), null, request.getPitch());

        try {
            long startTime = System.currentTimeMillis();
            while (true) {
                final long TIMEOUT_MS = 10000;
                if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                    break;
                }
                byte[] chunk = audioDataQueue.poll(100, TimeUnit.MILLISECONDS);
                if (chunk != null) {
                    int offset = 0;
                    while (offset < chunk.length) {
                        int chunkSize = Math.min(callback.getMaxBufferSize(), chunk.length - offset);
                        callback.audioAvailable(chunk, offset, chunkSize);
                        offset += chunkSize;
                    }
                }
                if (isAudioQueueDone.get()) break;
            }
            callback.done();
        } catch (Exception e) {
            callback.error();
        }
        synthesisEngine.destroy();
    }

    public static int getIsLanguageAvailable(String lang, String country, String variant) {
        Locale locale = new Locale(lang, country, variant);
        boolean isLanguage = false;
        boolean isCountry = false;
        for (String lan : Constants.supportedLanguages) {
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
