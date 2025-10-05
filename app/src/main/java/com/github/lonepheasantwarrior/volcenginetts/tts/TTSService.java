package com.github.lonepheasantwarrior.volcenginetts.tts;

import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;

import com.github.lonepheasantwarrior.volcenginetts.TTSApplication;
import com.github.lonepheasantwarrior.volcenginetts.common.Constants;
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngine;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class TTSService extends TextToSpeechService {
    @Nullable
    private volatile String[] mCurrentLanguage = null;

    private final SynthesisEngine synthesisEngine = ((TTSApplication) getApplicationContext()).getSynthesisEngine();

    @Override
    protected String[] onGetLanguage() {
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
}
