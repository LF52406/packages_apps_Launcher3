/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.util

import android.content.Context
import com.android.launcher3.LauncherPrefs

/** Package and Android-user scoped choices; doesn't modify the application's normal behavior. */
object RecentsPrivacyPrefs {
    private fun appKey(packageName: String, userId: Int) = "$userId:$packageName"

    @JvmStatic
    fun isProtected(context: Context, packageName: String, userId: Int): Boolean =
        LauncherPrefs.get(context).get(LauncherPrefs.RECENTS_PRIVACY_APPS)
            .contains(appKey(packageName, userId))

    @JvmStatic
    fun setProtected(context: Context, packageName: String, userId: Int, enabled: Boolean) {
        val prefs = LauncherPrefs.get(context)
        val apps = prefs.get(LauncherPrefs.RECENTS_PRIVACY_APPS).toMutableSet()
        val key = appKey(packageName, userId)
        if (enabled) apps.add(key) else apps.remove(key)
        prefs.put(LauncherPrefs.RECENTS_PRIVACY_APPS, apps.toSet())
    }
}
