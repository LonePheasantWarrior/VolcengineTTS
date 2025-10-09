package com.github.lonepheasantwarrior.volcenginetts.tts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.github.lonepheasantwarrior.volcenginetts.R;

import java.util.Locale;


public class GetSampleText extends Activity {

    private static final String TAG = GetSampleText.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int result = TextToSpeech.LANG_AVAILABLE;
        Intent returnData = new Intent();

        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        if (bundle == null) {
            // 即使没有bundle，也要设置默认示例文本
            returnData.putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, getString(R.string.tts_sample_default));
            setResult(result, returnData);
            finish();
            return;
        }

        String language = bundle.getString("language");
        String country = bundle.getString("country");
        String variant = bundle.getString("variant");
        Log.d(TAG, language + "_" + country + "_" + variant);

        if (language == null || country == null) {
            // 即使缺少语言或国家信息，也要设置默认示例文本
            returnData.putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, getString(R.string.tts_sample_default));
            setResult(result, returnData);
            finish();
            return;
        }

        try {
            Locale locale = new Locale(language, country);
            returnData.putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, TtsVoiceSample.getByLocate(this, locale));
        } catch (Exception e) {
            // 捕获所有可能的异常，确保返回默认文本
            Log.e(TAG, "获取特定语言的示例文本失败: " + e.getMessage());
            returnData.putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, getString(R.string.tts_sample_default));
        }

        setResult(result, returnData);
        finish();
    }

    /**
     * 根据当前环境获取合适的TTS演示文本
     *
     * @return TTS演示文本
     */
    public String getRawSampleText() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return getString(R.string.tts_sample_default);
        }

        String language = bundle.getString("language");
        String country = bundle.getString("country");

        if (language == null || country == null) {
            return getString(R.string.tts_sample_default);
        }
        return TtsVoiceSample.getByLocate(this, new Locale(language, country));
    }
}