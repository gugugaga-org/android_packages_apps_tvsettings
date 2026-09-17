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

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.settings.data.ConstData;
import com.android.tv.settings.util.ReflectUtils;

/**
 * Over-scan (zoom) settings for the Rockchip display device screen.  Shown as
 * a preference screen so the two-panel framework can display it in either
 * panel.
 */
public class ScreenScaleFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "ScreenScaleFragment";

    private static final String KEY_HORIZONTAL = "scale_horizontal";
    private static final String KEY_VERTICAL = "scale_vertical";
    private static final String KEY_RESET = "scale_reset";

    private static final int MIN_SCALE = 80;
    private static final int MAX_SCALE = 100;
    private static final int OVERSCAN_LEFT = 0;
    private static final int OVERSCAN_TOP = 1;
    private static final int OVERSCAN_RIGHT = 2;
    private static final int OVERSCAN_BOTTOM = 3;

    private Object mDisplayManager;
    private int mDisplayId;
    private int mHorizontalScale = MAX_SCALE;
    private int mVerticalScale = MAX_SCALE;

    private SeekBarPreference mHorizontalPreference;
    private SeekBarPreference mVerticalPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.screen_scale, null);

        Bundle args = getArguments();
        if (args != null) {
            DisplayInfo displayInfo = (DisplayInfo) args.getSerializable(
                    ConstData.IntentKey.DISPLAY_INFO);
            if (displayInfo != null) {
                mDisplayId = displayInfo.getDisplayId();
            } else {
                mDisplayId = args.getInt(ConstData.IntentKey.DISPLAY_ID, 0);
            }
        }

        try {
            mDisplayManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
        } catch (Exception e) {
            Log.e(TAG, "RkDisplayOutputManager is not available", e);
        }

        mHorizontalPreference = findPreference(KEY_HORIZONTAL);
        mVerticalPreference = findPreference(KEY_VERTICAL);
        Preference resetPreference = findPreference(KEY_RESET);
        if (mHorizontalPreference == null || mVerticalPreference == null) {
            return;
        }
        mHorizontalPreference.setMax(MAX_SCALE - MIN_SCALE);
        mVerticalPreference.setMax(MAX_SCALE - MIN_SCALE);
        mHorizontalPreference.setOnPreferenceChangeListener(this);
        mVerticalPreference.setOnPreferenceChangeListener(this);
        if (resetPreference != null) {
            resetPreference.setOnPreferenceClickListener(this);
        }

        Rect overScan = null;
        if (mDisplayManager != null) {
            overScan = (Rect) ReflectUtils.invokeMethod(mDisplayManager, "getOverScan",
                    new Class[] { int.class }, new Object[] { mDisplayId });
        }
        if (overScan != null) {
            mHorizontalScale = clamp(overScan.left);
            mVerticalScale = clamp(overScan.bottom);
        }
        mHorizontalPreference.setValue(mHorizontalScale - MIN_SCALE);
        mVerticalPreference.setValue(mVerticalScale - MIN_SCALE);
        updateSummaries();
    }

    private int clamp(int value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    private void updateSummaries() {
        mHorizontalPreference.setSummary(
                getString(R.string.display_scale_value, mHorizontalScale));
        mVerticalPreference.setSummary(getString(R.string.display_scale_value, mVerticalScale));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHorizontalPreference) {
            mHorizontalScale = clamp((Integer) newValue + MIN_SCALE);
            applyOverScan(OVERSCAN_LEFT, mHorizontalScale);
            applyOverScan(OVERSCAN_RIGHT, mHorizontalScale);
            updateSummaries();
            saveConfig();
        } else if (preference == mVerticalPreference) {
            mVerticalScale = clamp((Integer) newValue + MIN_SCALE);
            applyOverScan(OVERSCAN_TOP, mVerticalScale);
            applyOverScan(OVERSCAN_BOTTOM, mVerticalScale);
            updateSummaries();
            saveConfig();
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (KEY_RESET.equals(preference.getKey())) {
            mHorizontalScale = MAX_SCALE;
            mVerticalScale = MAX_SCALE;
            mHorizontalPreference.setValue(MAX_SCALE - MIN_SCALE);
            mVerticalPreference.setValue(MAX_SCALE - MIN_SCALE);
            applyOverScan(OVERSCAN_LEFT, mHorizontalScale);
            applyOverScan(OVERSCAN_RIGHT, mHorizontalScale);
            applyOverScan(OVERSCAN_TOP, mVerticalScale);
            applyOverScan(OVERSCAN_BOTTOM, mVerticalScale);
            updateSummaries();
            saveConfig();
        }
        return true;
    }

    private void applyOverScan(int direction, int value) {
        if (mDisplayManager == null) {
            return;
        }
        ReflectUtils.invokeMethod(mDisplayManager, "setOverScan",
                new Class[] { int.class, int.class, int.class },
                new Object[] { mDisplayId, direction, value });
    }

    private void saveConfig() {
        if (mDisplayManager == null) {
            return;
        }
        ReflectUtils.invokeMethod(mDisplayManager, "saveConfig", new Class[] {}, new Object[] {});
    }
}
