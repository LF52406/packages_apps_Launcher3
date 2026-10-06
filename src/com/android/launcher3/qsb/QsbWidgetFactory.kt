/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.launcher3.qsb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R
import com.android.launcher3.Utilities
import javax.inject.Inject

/** Wrapper class for qsb widget inflation to allow easier override */
open class QsbWidgetFactory @Inject constructor() {

    open fun createView(container: ViewGroup): View {
        val context = container.context
        val useGoogleStyle =
            LauncherPrefs.get(context).get(LauncherPrefs.QSB_STYLE_GOOGLE) &&
                Utilities.isGSAEnabled(context)
        val qsbLayout = if (useGoogleStyle) {
            R.layout.search_container_hotseat_google
        } else {
            R.layout.search_container_hotseat
        }
        return LayoutInflater.from(context).inflate(qsbLayout, container, false)
    }
}
