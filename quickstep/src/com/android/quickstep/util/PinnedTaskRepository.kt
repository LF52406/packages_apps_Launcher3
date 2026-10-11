/*
 * Copyright (C) 2026 The MistOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.util

import android.content.Context
import com.android.quickstep.views.TaskView
import com.android.systemui.shared.recents.model.Task

/**
 * User-selected app locks for Overview. This is NOT Android's screen-pinning/lock-task mode,
 * and does not keep processes alive or override the system's background restrictions.
 *
 * Stored by Android user ID and package name rather than task ID, since task IDs are ephemeral.
 */
object PinnedTaskRepository {
    private const val PREFERENCES = "mist_recents_pinned_apps"
    private const val PINNED_APPS = "pinned_apps"
    private val mutationLock = Any()

    private fun taskKey(task: Task?): String? {
        if (task == null || task.key.id < 0) return null
        val packageName = task.key.baseIntent.component?.packageName
            ?: task.topComponent?.packageName
            ?: task.key.baseIntent.`package`
            ?: return null
        return "${task.key.userId}:$packageName"
    }

    @JvmStatic
    fun canPin(task: Task?): Boolean = taskKey(task) != null

    @JvmStatic
    fun hasPinnedApps(context: Context): Boolean = pins(context).isNotEmpty()

    @JvmStatic
    fun isPinned(context: Context, task: Task?): Boolean {
        val key = taskKey(task) ?: return false
        return pins(context).contains(key)
    }

    @JvmStatic
    fun isTaskViewPinned(context: Context, taskView: TaskView?): Boolean =
        taskView?.taskContainers?.any { isPinned(context, it.task) } == true

    @JvmStatic
    fun setPinned(context: Context, task: Task?, pinned: Boolean): Boolean {
        val key = taskKey(task) ?: return false
        synchronized(mutationLock) {
            val preferences =
                context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            // Never modify the Set returned by SharedPreferences in place.
            val keys = preferences.getStringSet(PINNED_APPS, emptySet()).orEmpty().toMutableSet()
            if (pinned) keys.add(key) else keys.remove(key)
            preferences.edit().putStringSet(PINNED_APPS, keys).apply()
        }
        return true
    }

    private fun pins(context: Context): Set<String> =
        context.applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getStringSet(PINNED_APPS, emptySet())
            .orEmpty()
}
