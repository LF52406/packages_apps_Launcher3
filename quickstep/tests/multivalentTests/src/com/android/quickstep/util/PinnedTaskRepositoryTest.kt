/*
 * Copyright (C) 2026 The MistOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.quickstep.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.systemui.shared.recents.model.Task
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinnedTaskRepositoryTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun task(packageName: String, id: Int, userId: Int): Task {
        val intent = Intent().setComponent(ComponentName(packageName, "$packageName.Main"))
        return Task(Task.TaskKey(id, 0, intent, null, userId, 0))
    }

    private val regular = task("org.mistos.recentspin.test", 10001, 0)
    private val sameAppNewTask = task("org.mistos.recentspin.test", 10002, 0)
    private val otherProfile = task("org.mistos.recentspin.test", 10003, 10)

    @After
    fun tearDown() {
        PinnedTaskRepository.setPinned(context, regular, false)
        PinnedTaskRepository.setPinned(context, otherProfile, false)
    }

    @Test
    fun pinIsPersistentAcrossTaskIdChanges() {
        assertThat(PinnedTaskRepository.setPinned(context, regular, true)).isTrue()
        assertThat(PinnedTaskRepository.isPinned(context, sameAppNewTask)).isTrue()
        assertThat(PinnedTaskRepository.setPinned(context, sameAppNewTask, false)).isTrue()
        assertThat(PinnedTaskRepository.isPinned(context, regular)).isFalse()
    }

    @Test
    fun pinIsScopedToAndroidUser() {
        PinnedTaskRepository.setPinned(context, regular, true)
        assertThat(PinnedTaskRepository.isPinned(context, otherProfile)).isFalse()
        PinnedTaskRepository.setPinned(context, otherProfile, true)
        PinnedTaskRepository.setPinned(context, regular, false)
        assertThat(PinnedTaskRepository.isPinned(context, otherProfile)).isTrue()
    }
}
