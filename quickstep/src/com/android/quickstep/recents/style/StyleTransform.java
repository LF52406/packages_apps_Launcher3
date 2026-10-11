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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.quickstep.views.TaskView;

public class StyleTransform {

    public float translationX;
    public float translationY;
    public float scale;
    public float rotation;
    public float elevation;
    public float alpha;

    public StyleTransform() {
        reset();
    }

    public void reset() {
        translationX = 0f;
        translationY = 0f;
        scale = 1f;
        rotation = 0f;
        elevation = 0f;
        alpha = 1f;
    }

    public void set(@NonNull StyleTransform other) {
        this.translationX = other.translationX;
        this.translationY = other.translationY;
        this.scale = other.scale;
        this.rotation = other.rotation;
        this.elevation = other.elevation;
        this.alpha = other.alpha;
    }

    public void applyTo(@Nullable TaskView taskView) {
        if (taskView == null) {
            return;
        }
        // Each setter may invalidate the view or recompute thumbnail fullscreen parameters.
        // Avoid doing that on every frame when a property did not actually change.
        if (taskView.getCustomStyleTranslationX() != translationX) {
            taskView.setCustomStyleTranslationX(translationX);
        }
        if (taskView.getCustomStyleTranslationY() != translationY) {
            taskView.setCustomStyleTranslationY(translationY);
        }
        if (taskView.getCustomStyleScale() != scale) {
            taskView.setCustomStyleScale(scale);
        }
        if (taskView.getRotation() != rotation) {
            taskView.setRotation(rotation);
        }
        if (taskView.getElevation() != elevation) {
            taskView.setElevation(elevation);
        }
        if (taskView.getCustomStyleAlpha() != alpha) {
            taskView.setCustomStyleAlpha(alpha);
        }
    }
}
