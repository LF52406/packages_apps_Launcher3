/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.qsb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.util.Themes;

public class QsbOuterDrawable extends Drawable {
    private final Paint mPaint;
    private final RectF mRect;
    private final Context mContext;

    public QsbOuterDrawable(Context context) {
        mContext = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRect = new RectF();
    }

    /** Converts the 0–100 opacity preference value to an 0–255 alpha, clamped to valid range. */
    private int getBackgroundAlpha() {
        int opacity = LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY.get(mContext);
        opacity = Math.max(0, Math.min(100, opacity));
        return Math.round(opacity * 255 / 100f);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;

        int alpha = getBackgroundAlpha();

        float radius = mContext.getResources().getDimensionPixelSize(
                com.android.launcher3.R.dimen.qsb_widget_height);

        // Outer shape — themed capsule background
        int themedColor = Themes.getAttrColor(mContext, com.android.launcher3.R.attr.qsbOuterColor);
        int outerColorWithAlpha = (themedColor & 0x00FFFFFF) | (alpha << 24);
        mPaint.setColor(outerColorWithAlpha);
        mRect.set(bounds);
        canvas.drawRoundRect(mRect, radius, radius, mPaint);

        // Inner search pill — only drawn when background is at least partially visible
        if (alpha > 0) {
            int padding = mContext.getResources().getDimensionPixelSize(
                    com.android.launcher3.R.dimen.qsb_widget_height) / 8;

            int aiModeButtonSize = mContext.getResources().getDimensionPixelSize(
                    com.android.launcher3.R.dimen.qsb_ai_mode_button_size);
            int aiModeButtonMargin = mContext.getResources().getDimensionPixelSize(
                    com.android.launcher3.R.dimen.qsb_ai_mode_button_margin);
            int marginEnd = mContext.getResources().getDimensionPixelSize(
                    com.android.launcher3.R.dimen.qsb_marginEnd);

            int rightPadding = aiModeButtonSize + aiModeButtonMargin + marginEnd - padding;

            Rect innerBounds = new Rect(
                    bounds.left + padding,
                    bounds.top + padding,
                    bounds.right - rightPadding,
                    bounds.bottom - padding
            );

            int fillColor = Themes.getAttrColor(mContext, com.android.launcher3.R.attr.qsbFillColor);
            int fillColorWithAlpha = (fillColor & 0x00FFFFFF) | (alpha << 24);
            mPaint.setColor(fillColorWithAlpha);

            RectF innerRect = new RectF(innerBounds);
            canvas.drawRoundRect(innerRect, radius, radius, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // Not used — opacity is controlled via HOTSEAT_QSB_GOOGLE_OPACITY preference
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Not used
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
