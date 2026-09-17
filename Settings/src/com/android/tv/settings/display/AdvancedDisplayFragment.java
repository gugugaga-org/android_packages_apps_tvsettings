/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tv.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.data.ConstData;
import com.android.tv.settings.util.ReflectUtils;

/**
 * BCSH (brightness/contrast/saturation/tone) settings for the Rockchip display
 * device screen.  Shown as a preference screen so the two-panel framework can
 * display it in either panel.
 */
public class AdvancedDisplayFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "AdvancedDisplayFragment";

    private static final String KEY_BRIGHTNESS = "bcsh_brightness";
    private static final String KEY_CONTRAST = "bcsh_contrast";
    private static final String KEY_SATURATION = "bcsh_saturation";
    private static final String KEY_TONE = "bcsh_tone";
    private static final String KEY_PRESET_RESET = "bcsh_preset_reset";
    private static final String KEY_PRESET_COLD = "bcsh_preset_cold";
    private static final String KEY_PRESET_WARM = "bcsh_preset_warm";
    private static final String KEY_PRESET_SHARP = "bcsh_preset_sharp";

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

    private SeekBarPreference mBrightnessPreference;
    private SeekBarPreference mContrastPreference;
    private SeekBarPreference mSaturationPreference;
    private SeekBarPreference mTonePreference;

    private int mBrightness = DEFAULT_VALUE;
    private int mContrast = DEFAULT_VALUE;
    private int mSaturation = DEFAULT_VALUE;
    private int mTone = DEFAULT_VALUE;

    private boolean mSuppressCallbacks;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.display_bcsh, null);

        Bundle args = getArguments();
        if (args != null) {
            mDisplayId = args.getInt(ConstData.IntentKey.DISPLAY_ID, 0);
        }

        try {
            mRkDisplayManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
        } catch (Exception e) {
            Log.e(TAG, "RkDisplayOutputManager is not available", e);
        }

        mBrightnessPreference = findPreference(KEY_BRIGHTNESS);
        mContrastPreference = findPreference(KEY_CONTRAST);
        mSaturationPreference = findPreference(KEY_SATURATION);
        mTonePreference = findPreference(KEY_TONE);
        for (SeekBarPreference preference : new SeekBarPreference[] {
                mBrightnessPreference, mContrastPreference,
                mSaturationPreference, mTonePreference }) {
            if (preference == null) {
                return;
            }
            preference.setOnPreferenceChangeListener(this);
        }
        for (String key : new String[] { KEY_PRESET_RESET, KEY_PRESET_COLD,
                KEY_PRESET_WARM, KEY_PRESET_SHARP }) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setOnPreferenceClickListener(this);
            }
        }

        mSuppressCallbacks = true;
        mBrightness = getDisplayValue("getBrightness");
        mContrast = getDisplayValue("getContrast");
        mSaturation = getDisplayValue("getSaturation");
        mTone = getDisplayValue("getHue");
        mBrightnessPreference.setValue(mBrightness);
        mContrastPreference.setValue(mContrast);
        mSaturationPreference.setValue(mSaturation);
        mTonePreference.setValue(mTone);
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
        setDisplayValue("setBrightness", mBrightness);
        setDisplayValue("setContrast", mContrast);
        setDisplayValue("setSaturation", mSaturation);
        setDisplayValue("setHue", mTone);
    }

    private void setPresetValues(int brightness, int contrast, int saturation, int tone) {
        mBrightness = brightness;
        mContrast = contrast;
        mSaturation = saturation;
        mTone = tone;
        mSuppressCallbacks = true;
        mBrightnessPreference.setValue(brightness);
        mContrastPreference.setValue(contrast);
        mSaturationPreference.setValue(saturation);
        mTonePreference.setValue(tone);
        mSuppressCallbacks = false;
        applyBcshValues();
        saveNewValue();
        DrmDisplaySetting.saveConfig();
    }

    private void saveNewValue() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        SharedPreferences bcshPreferences = context.getSharedPreferences(
                ConstData.SharedKey.BCSH_VALUES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = bcshPreferences.edit();
        editor.putInt(ConstData.SharedKey.BCSH_BRIGHTNESS, mBrightness);
        editor.putInt(ConstData.SharedKey.BCSH_CONTRAST, mContrast);
        editor.putInt(ConstData.SharedKey.BCSH_STAURATION, mSaturation);
        editor.putInt(ConstData.SharedKey.BCSH_TONE, mTone);
        editor.apply();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mSuppressCallbacks) {
            return true;
        }
        int value = (Integer) newValue;
        if (preference == mBrightnessPreference) {
            mBrightness = value;
        } else if (preference == mContrastPreference) {
            mContrast = value;
        } else if (preference == mSaturationPreference) {
            mSaturation = value;
        } else if (preference == mTonePreference) {
            mTone = value;
        }
        applyBcshValues();
        saveNewValue();
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_PRESET_RESET.equals(key)) {
            setPresetValues(RESET_VALUE, RESET_VALUE, RESET_VALUE, RESET_VALUE);
        } else if (KEY_PRESET_COLD.equals(key)) {
            setPresetValues(COLD_BRIGHTNESS, COLD_CONTRAST, COLD_SATURATION, COLD_TONE);
        } else if (KEY_PRESET_WARM.equals(key)) {
            setPresetValues(WARM_BRIGHTNESS, WARM_CONTRAST, WARM_SATURATION, WARM_TONE);
        } else if (KEY_PRESET_SHARP.equals(key)) {
            setPresetValues(SHARP_BRIGHTNESS, SHARP_CONTRAST, SHARP_SATURATION, SHARP_TONE);
        }
        return true;
    }
}
