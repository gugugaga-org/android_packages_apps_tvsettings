package com.android.tv.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.FragmentActivity;

import com.android.tv.settings.R;
import com.android.tv.settings.data.ConstData;
import com.android.tv.settings.util.ReflectUtils;

public class AdvancedDisplaySettingsActivity extends FragmentActivity implements View.OnClickListener {
    private static final String TAG = "AdvancedDisplaySettingsActivity";

    private static final int DEFAULT_VALUE = 50;
    private static final int RESET_VALUE = 50;
    private static final int COLD_BRIGHTNESS = 50;
    private static final int COLD_CONTRAST = 50;
    private static final int COLD_SATURATION = 30;
    private static final int COLD_TONE = 31;
    private static final int WARM_BRIGHTNESS = 67;
    private static final int WARM_CONTRAST = 53;
    private static final int WARM_SATURATION = 80;
    private static final int WARM_TONE = 74;
    private static final int SHARP_BRIGHTNESS = 60;
    private static final int SHARP_CONTRAST = 77;
    private static final int SHARP_SATURATION = 43;
    private static final int SHARP_TONE = 46;

    private Object mRkDisplayManager;
    private int mDisplayId;

    private AppCompatSeekBar mBrightnessSeekBar;
    private AppCompatSeekBar mContrastSeekBar;
    private AppCompatSeekBar mSaturationSeekBar;
    private AppCompatSeekBar mToneSeekBar;
    private TextView mBrightnessValue;
    private TextView mContrastValue;
    private TextView mSaturationValue;
    private TextView mToneValue;
    private View mBrightnessRow;
    private View mContrastRow;
    private View mSaturationRow;
    private View mToneRow;

    private int mOldBcshBrightness = DEFAULT_VALUE;
    private int mOldBcshContrast = DEFAULT_VALUE;
    private int mOldBcshSaturation = DEFAULT_VALUE;
    private int mOldBcshTone = DEFAULT_VALUE;

    private boolean mSuppressCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_display);
        mDisplayId = getIntent().getIntExtra(ConstData.IntentKey.DISPLAY_ID, 0);
        try {
            mRkDisplayManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
        } catch (Exception e) {
            Log.e(TAG, "RkDisplayOutputManager is not available", e);
        }
        initViews();
        loadCurrentValues();
        updateValueLabels();
    }

    private void initViews() {
        mBrightnessSeekBar = findViewById(R.id.brightness);
        mContrastSeekBar = findViewById(R.id.contrast);
        mSaturationSeekBar = findViewById(R.id.saturation);
        mToneSeekBar = findViewById(R.id.tone);
        mBrightnessValue = findViewById(R.id.brightness_value);
        mContrastValue = findViewById(R.id.contrast_value);
        mSaturationValue = findViewById(R.id.saturation_value);
        mToneValue = findViewById(R.id.tone_value);
        mBrightnessRow = findViewById(R.id.brightness_row);
        mContrastRow = findViewById(R.id.contrast_row);
        mSaturationRow = findViewById(R.id.saturation_row);
        mToneRow = findViewById(R.id.tone_row);

        for (AppCompatSeekBar seekBar : new AppCompatSeekBar[] {
                mBrightnessSeekBar, mContrastSeekBar, mSaturationSeekBar, mToneSeekBar }) {
            seekBar.setMax(100);
            seekBar.setKeyProgressIncrement(1);
            seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        }
        mBrightnessSeekBar.setOnFocusChangeListener((v, hasFocus) -> mBrightnessRow.setActivated(hasFocus));
        mContrastSeekBar.setOnFocusChangeListener((v, hasFocus) -> mContrastRow.setActivated(hasFocus));
        mSaturationSeekBar.setOnFocusChangeListener((v, hasFocus) -> mSaturationRow.setActivated(hasFocus));
        mToneSeekBar.setOnFocusChangeListener((v, hasFocus) -> mToneRow.setActivated(hasFocus));

        findViewById(R.id.preset_reset).setOnClickListener(this);
        findViewById(R.id.preset_cold).setOnClickListener(this);
        findViewById(R.id.preset_warm).setOnClickListener(this);
        findViewById(R.id.preset_sharp).setOnClickListener(this);
        findViewById(R.id.advanced_cancel).setOnClickListener(this);
        findViewById(R.id.advanced_confirm).setOnClickListener(this);
    }

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateValueLabels();
            if (!mSuppressCallbacks) {
                applyBcshValues();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private void loadCurrentValues() {
        mSuppressCallbacks = true;
        mOldBcshBrightness = getDisplayValue("getBrightness");
        mOldBcshContrast = getDisplayValue("getContrast");
        mOldBcshSaturation = getDisplayValue("getSaturation");
        mOldBcshTone = getDisplayValue("getHue");
        mBrightnessSeekBar.setProgress(mOldBcshBrightness);
        mContrastSeekBar.setProgress(mOldBcshContrast);
        mSaturationSeekBar.setProgress(mOldBcshSaturation);
        mToneSeekBar.setProgress(mOldBcshTone);
        mSuppressCallbacks = false;
    }

    private int getDisplayValue(String method) {
        if (mRkDisplayManager == null) {
            return DEFAULT_VALUE;
        }
        Object value = ReflectUtils.invokeMethod(mRkDisplayManager, method,
                new Class[] { int.class }, new Object[] { mDisplayId });
        return value instanceof Integer ? (Integer) value : DEFAULT_VALUE;
    }

    private void setDisplayValue(String method, int value) {
        if (mRkDisplayManager == null) {
            return;
        }
        ReflectUtils.invokeMethod(mRkDisplayManager, method,
                new Class[] { int.class, int.class }, new Object[] { mDisplayId, value });
    }

    private void applyBcshValues() {
        setDisplayValue("setBrightness", mBrightnessSeekBar.getProgress());
        setDisplayValue("setContrast", mContrastSeekBar.getProgress());
        setDisplayValue("setSaturation", mSaturationSeekBar.getProgress());
        setDisplayValue("setHue", mToneSeekBar.getProgress());
    }

    private void updateValueLabels() {
        mBrightnessValue.setText(String.valueOf(mBrightnessSeekBar.getProgress()));
        mContrastValue.setText(String.valueOf(mContrastSeekBar.getProgress()));
        mSaturationValue.setText(String.valueOf(mSaturationSeekBar.getProgress()));
        mToneValue.setText(String.valueOf(mToneSeekBar.getProgress()));
    }

    private void setPresetValues(int brightness, int contrast, int saturation, int tone) {
        mBrightnessSeekBar.setProgress(brightness);
        mContrastSeekBar.setProgress(contrast);
        mSaturationSeekBar.setProgress(saturation);
        mToneSeekBar.setProgress(tone);
        saveNewValue();
        DrmDisplaySetting.saveConfig();
    }

    private void recoveryOldValue() {
        setDisplayValue("setBrightness", mOldBcshBrightness);
        setDisplayValue("setContrast", mOldBcshContrast);
        setDisplayValue("setSaturation", mOldBcshSaturation);
        setDisplayValue("setHue", mOldBcshTone);
    }

    private void saveNewValue() {
        SharedPreferences bcshPreferences = getSharedPreferences(ConstData.SharedKey.BCSH_VALUES,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = bcshPreferences.edit();
        editor.putInt(ConstData.SharedKey.BCSH_BRIGHTNESS, mBrightnessSeekBar.getProgress());
        editor.putInt(ConstData.SharedKey.BCSH_CONTRAST, mContrastSeekBar.getProgress());
        editor.putInt(ConstData.SharedKey.BCSH_STAURATION, mSaturationSeekBar.getProgress());
        editor.putInt(ConstData.SharedKey.BCSH_TONE, mToneSeekBar.getProgress());
        editor.apply();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.advanced_confirm) {
            saveNewValue();
            DrmDisplaySetting.saveConfig();
            finish();
        } else if (id == R.id.advanced_cancel) {
            recoveryOldValue();
            finish();
        } else if (id == R.id.preset_reset) {
            setPresetValues(RESET_VALUE, RESET_VALUE, RESET_VALUE, RESET_VALUE);
        } else if (id == R.id.preset_cold) {
            setPresetValues(COLD_BRIGHTNESS, COLD_CONTRAST, COLD_SATURATION, COLD_TONE);
        } else if (id == R.id.preset_warm) {
            setPresetValues(WARM_BRIGHTNESS, WARM_CONTRAST, WARM_SATURATION, WARM_TONE);
        } else if (id == R.id.preset_sharp) {
            setPresetValues(SHARP_BRIGHTNESS, SHARP_CONTRAST, SHARP_SATURATION, SHARP_TONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            recoveryOldValue();
        }
        return super.onKeyDown(keyCode, event);
    }
}
