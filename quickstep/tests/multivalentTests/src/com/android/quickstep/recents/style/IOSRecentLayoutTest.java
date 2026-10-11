/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.recents.style;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the continuously evaluated geometry of the iOS-style Recents carousel. */
@RunWith(AndroidJUnit4.class)
public class IOSRecentLayoutTest {
    @Test
    public void focusedCardHasIdentityScaleAndCenteredTranslation() {
        assertThat(IOSRecentLayout.getVisualDistance(0f, 1000f)).isWithin(0.0001f).of(0f);
        assertThat(IOSRecentLayout.getScaleForDistance(0f)).isWithin(0.0001f).of(1f);
        assertThat(IOSRecentLayout.getAlphaForDistance(0f)).isWithin(0.0001f).of(1f);
    }

    @Test
    public void distanceAndScaleAreMonotonicAndFinite() {
        float previousDistance = -1f;
        float previousScale = 2f;
        for (int i = 0; i <= 400; i++) {
            float d = i / 100f;
            float offset = IOSRecentLayout.getVisualDistance(d, 1000f);
            float scale = IOSRecentLayout.getScaleForDistance(d);
            assertThat(Float.isFinite(offset)).isTrue();
            assertThat(Float.isFinite(scale)).isTrue();
            assertThat(offset).isAtLeast(previousDistance);
            assertThat(scale).isAtMost(previousScale);
            assertThat(scale).isAtLeast(0.85f);
            previousDistance = offset;
            previousScale = scale;
        }
    }

    @Test
    public void noKinkAtIntegerPageDistances() {
        final float h = 0.001f;
        for (int page = 1; page <= 3; page++) {
            float left = (IOSRecentLayout.getVisualDistance(page, 1000f)
                    - IOSRecentLayout.getVisualDistance(page - h, 1000f)) / h;
            float right = (IOSRecentLayout.getVisualDistance(page + h, 1000f)
                    - IOSRecentLayout.getVisualDistance(page, 1000f)) / h;
            assertThat(Math.abs(left - right)).isLessThan(2f);
        }
    }

    @Test
    public void farCardsFadeOutWithoutAlphaJump() {
        assertThat(IOSRecentLayout.getAlphaForDistance(2.65f)).isEqualTo(1f);
        assertThat(IOSRecentLayout.getAlphaForDistance(3.50f)).isEqualTo(0f);
        for (int i = 0; i <= 500; i++) {
            float alpha = IOSRecentLayout.getAlphaForDistance(i / 100f);
            assertThat(alpha).isAtLeast(0f);
            assertThat(alpha).isAtMost(1f);
        }
    }
}
