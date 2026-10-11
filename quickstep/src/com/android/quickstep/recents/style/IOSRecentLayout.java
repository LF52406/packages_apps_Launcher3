/*
 * Copyright (C) 2026 MistOS
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
 * limitations under the License.
 */

package com.android.quickstep.recents.style;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

/**
 * iOS-inspired, overlapped Overview carousel.
 *
 * Transformations are derived directly from the laid-out card center and the current viewport
 * center. This avoids mismatches between the platform's page spacing / clamped page offsets and
 * the position of a rendered card, and works in both LTR and RTL without double mirroring.
 *
 * No independent per-card animators: the layout follows the real gesture/scroller position on
 * every frame. The mapping is continuously differentiable, including at card centers.
 */
public final class IOSRecentLayout implements RecentLayoutHandler {

    private static final float MIN_SCALE = 0.85f;
    private static final float SCALE_RANGE = 1f - MIN_SCALE;
    private static final float VISUAL_SPREAD = 0.96f;
    private static final float SPREAD_DECAY = 0.60f;
    private static final float NEAR_FADE_START = 2.65f;
    private static final float FAR_FADE_END = 3.50f;
    private static final float VISIBLE_DISTANCE = 3.55f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.IOS;
    }

    @Override
    public void onDetached(@NonNull RecentsView<?, ?> recentsView) {
        for (TaskView task : recentsView.getTaskViews()) {
            task.setColorTint(0f, 0);
        }
    }

    @Nullable
    @Override
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        if (recentsView.indexOfChild(taskView) < 0) return false;
        if (taskView.getWidth() <= 0 || recentsView.getWidth() <= 0
                || recentsView.getLastComputedTaskSize().width() <= 0) return true;
        return Math.abs(getNativeCenterOffset(recentsView, taskView))
                / getNominalPageSize(recentsView) < VISIBLE_DISTANCE;
    }

    @Override
    public void calculateTransform(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView, int taskIndex, int taskCount, float scrollProgress,
            @NonNull StyleTransform outTransform) {
        outTransform.reset();

        int cardWidth = recentsView.getLastComputedTaskSize().width();
        if (cardWidth <= 0 || taskView.getWidth() <= 0 || recentsView.getWidth() <= 0) {
            return;
        }

        // Child left/top positions are untransformed layout coordinates. This geometric offset
        // stays correct while the parent is being scaled for app launch or Overview entry.
        final float nativeOffset = getNativeCenterOffset(recentsView, taskView);
        final float distance = Math.abs(nativeOffset) / getNominalPageSize(recentsView);

        if (distance >= VISIBLE_DISTANCE) {
            // Don't apply enormous offscreen translations or repeatedly tint hidden thumbnails.
            outTransform.scale = MIN_SCALE;
            outTransform.elevation = 0f;
            outTransform.alpha = 0f;
            return;
        }

        final float visualOffset = getVisualDistance(distance, cardWidth);
        final float targetOffset = Math.copySign(visualOffset, nativeOffset);
        final float density = recentsView.getResources().getDisplayMetrics().density;

        outTransform.translationX = targetOffset - nativeOffset;
        outTransform.translationY = 0f;
        outTransform.scale = getScaleForDistance(distance);
        outTransform.elevation = getElevationForDistance(distance, density);
        outTransform.alpha = getAlphaForDistance(distance);
    }

    private static float getNominalPageSize(@NonNull RecentsView<?, ?> recentsView) {
        int cardWidth = recentsView.getLastComputedTaskSize().width();
        return Math.max(1f, cardWidth + recentsView.getPageSpacing());
    }

    /**
     * Mirrors PagedView's screen-center calculation, including its scale and pivot, so the
     * geometry remains anchored during the app-to-Overview transition.
     */
    private static float getNativeCenterOffset(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        final float parentScale = Math.max(0.01f, recentsView.getScaleX());
        final float pivot = recentsView.getPivotX();
        final float viewportCenter =
                (recentsView.getWidth() * 0.5f - pivot) / parentScale + pivot;
        // Exclude the previous iOS transform, but preserve any ordinary AOSP launch/gesture
        // translation. This makes the result stable when Recents moves under a running animation.
        final float baseTranslation =
                taskView.getTranslationX() - taskView.getCustomStyleTranslationX();
        return (taskView.getLeft() + taskView.getWidth() * 0.5f + baseTranslation)
                - (recentsView.getScrollX() + viewportCenter);
    }

    // Saturating exponential gives a smooth, continuous visual spacing for any gesture position,
    // instead of piecewise linear offsets that produce velocity jumps at +/-1, +/-2 and +/-3.
    static float getVisualDistance(float distance, float cardWidth) {
        return VISUAL_SPREAD * cardWidth *
                (1f - (float) Math.exp(-SPREAD_DECAY * Math.max(0f, distance)));
    }

    // sqrt(1 + d²) - 1 has a zero first derivative at the focused card (d=0), preventing
    // a visible kink when the center card changes direction during a drag or snap.
    static float getScaleForDistance(float distance) {
        float d = Math.max(0f, distance);
        float curve = (float) Math.sqrt(1f + d * d) - 1f;
        return MIN_SCALE + SCALE_RANGE * (float) Math.exp(-0.80f * curve);
    }

    static float getElevationForDistance(float distance, float density) {
        float d = Math.max(0f, distance);
        float curve = (float) Math.sqrt(1f + d * d) - 1f;
        return (4f + 28f * (float) Math.exp(-1.2f * curve)) * density;
    }

    static float getAlphaForDistance(float distance) {
        if (distance <= NEAR_FADE_START) return 1f;
        if (distance >= FAR_FADE_END) return 0f;
        float t = (distance - NEAR_FADE_START) / (FAR_FADE_END - NEAR_FADE_START);
        float smoothstep = t * t * (3f - 2f * t);
        return 1f - smoothstep;
    }
}
