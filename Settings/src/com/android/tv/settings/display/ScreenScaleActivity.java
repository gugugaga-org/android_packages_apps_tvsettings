package com.android.tv.settings.display;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.FragmentActivity;

import com.android.tv.settings.R;
import com.android.tv.settings.data.ConstData;
import com.android.tv.settings.util.ReflectUtils;

public class ScreenScaleActivity extends FragmentActivity {
    private static final String TAG = "ScreenScaleActivity";

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

    private FrameLayout mPreviewFrame;
    private View mPreviewInner;
    private View mHorizontalRow;
    private View mVerticalRow;
    private TextView mHorizontalValue;
    private TextView mVerticalValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_scale);
        initDisplayManager();
        initData();
        initViews();
        refreshLabels();
        refreshPreview();
    }

    private void initDisplayManager() {
        try {
            mDisplayManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
        } catch (Exception e) {
            Log.e(TAG, "RkDisplayOutputManager is not available", e);
        }
    }

    private void initData() {
        DisplayInfo displayInfo = (DisplayInfo) getIntent()
                .getSerializableExtra(ConstData.IntentKey.DISPLAY_INFO);
        mDisplayId = displayInfo != null ? displayInfo.getDisplayId()
                : getIntent().getIntExtra(ConstData.IntentKey.DISPLAY_ID, 0);
        if (mDisplayManager == null) {
            return;
        }
        Rect overScan = (Rect) ReflectUtils.invokeMethod(mDisplayManager, "getOverScan",
                new Class[] { int.class }, new Object[] { mDisplayId });
        if (overScan == null) {
            return;
        }
        mHorizontalScale = clamp(overScan.left);
        mVerticalScale = clamp(overScan.bottom);
    }

    private int clamp(int value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    private void initViews() {
        mPreviewFrame = findViewById(R.id.preview_frame);
        mPreviewInner = findViewById(R.id.preview_inner);
        mHorizontalRow = findViewById(R.id.scale_horizontal_row);
        mVerticalRow = findViewById(R.id.scale_vertical_row);
        mHorizontalValue = findViewById(R.id.scale_horizontal_value);
        mVerticalValue = findViewById(R.id.scale_vertical_value);

        AppCompatSeekBar horizontalBar = findViewById(R.id.scale_horizontal);
        AppCompatSeekBar verticalBar = findViewById(R.id.scale_vertical);
        horizontalBar.setMax(MAX_SCALE - MIN_SCALE);
        verticalBar.setMax(MAX_SCALE - MIN_SCALE);
        horizontalBar.setKeyProgressIncrement(1);
        verticalBar.setKeyProgressIncrement(1);
        horizontalBar.setProgress(mHorizontalScale - MIN_SCALE);
        verticalBar.setProgress(mVerticalScale - MIN_SCALE);

        horizontalBar.setOnSeekBarChangeListener(new ScaleChangeListener(true));
        verticalBar.setOnSeekBarChangeListener(new ScaleChangeListener(false));
        horizontalBar.setOnFocusChangeListener((v, hasFocus) -> mHorizontalRow.setActivated(hasFocus));
        verticalBar.setOnFocusChangeListener((v, hasFocus) -> mVerticalRow.setActivated(hasFocus));

        findViewById(R.id.scale_reset).setOnClickListener(v -> {
            horizontalBar.setProgress(MAX_SCALE - MIN_SCALE);
            verticalBar.setProgress(MAX_SCALE - MIN_SCALE);
        });
        findViewById(R.id.scale_done).setOnClickListener(v -> finish());
    }

    private class ScaleChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final boolean mHorizontal;

        ScaleChangeListener(boolean horizontal) {
            mHorizontal = horizontal;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mHorizontal) {
                mHorizontalScale = clamp(progress + MIN_SCALE);
                applyOverScan(OVERSCAN_LEFT, mHorizontalScale);
                applyOverScan(OVERSCAN_RIGHT, mHorizontalScale);
            } else {
                mVerticalScale = clamp(progress + MIN_SCALE);
                applyOverScan(OVERSCAN_TOP, mVerticalScale);
                applyOverScan(OVERSCAN_BOTTOM, mVerticalScale);
            }
            refreshLabels();
            refreshPreview();
            saveConfig();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
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

    private void refreshLabels() {
        mHorizontalValue.setText(getString(R.string.display_scale_value, mHorizontalScale));
        mVerticalValue.setText(getString(R.string.display_scale_value, mVerticalScale));
    }

    private void refreshPreview() {
        mPreviewFrame.post(() -> {
            int width = mPreviewFrame.getWidth();
            int height = mPreviewFrame.getHeight();
            if (width == 0 || height == 0) {
                return;
            }
            int horizontalMargin = Math.round(width * (MAX_SCALE - mHorizontalScale) / 200f);
            int verticalMargin = Math.round(height * (MAX_SCALE - mVerticalScale) / 200f);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mPreviewInner.getLayoutParams();
            params.leftMargin = horizontalMargin;
            params.rightMargin = horizontalMargin;
            params.topMargin = verticalMargin;
            params.bottomMargin = verticalMargin;
            mPreviewInner.setLayoutParams(params);
        });
    }
}
