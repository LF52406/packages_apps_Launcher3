/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.launcher3.R;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.RecentsPrivacyPrefs;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Configure per-app Recents privacy without affecting the app window itself. */
public final class RecentsPrivacySettings extends CollapsingToolbarBaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new AppsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static final class AppsFragment extends SettingsBasePreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Context appContext = requireContext().getApplicationContext();
            final PreferenceScreen screen =
                    getPreferenceManager().createPreferenceScreen(requireContext());
            setPreferenceScreen(screen);
            final android.os.Handler handler =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            Executors.MODEL_EXECUTOR.execute(() -> {
                final List<AppEntry> apps = loadApps(appContext);
                handler.post(() -> {
                    if (!isAdded() || getPreferenceScreen() != screen) return;
                    if (apps.isEmpty()) {
                        final Preference empty = new Preference(requireContext());
                        empty.setTitle(R.string.recents_privacy_no_apps);
                        empty.setSelectable(false);
                        screen.addPreference(empty);
                        return;
                    }
                    for (AppEntry app : apps) {
                        final SwitchPreferenceCompat pref =
                                new SwitchPreferenceCompat(requireContext());
                        pref.setKey("recents_privacy_" + app.userId + "_" + app.packageName);
                        pref.setTitle(app.label);
                        pref.setSummary(app.packageName);
                        pref.setPersistent(false);
                        pref.setChecked(RecentsPrivacyPrefs.isProtected(appContext,
                                app.packageName, app.userId));
                        try {
                            pref.setIcon(app.info.getBadgedIcon(0));
                        } catch (RuntimeException ignored) {
                            // The app may have been removed while the list was being built.
                        }
                        pref.setOnPreferenceChangeListener((preference, newValue) -> {
                            RecentsPrivacyPrefs.setProtected(appContext, app.packageName,
                                    app.userId, (Boolean) newValue);
                            return true;
                        });
                        screen.addPreference(pref);
                    }
                });
            });
        }

        private static List<AppEntry> loadApps(Context context) {
            final LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            final Map<String, AppEntry> found = new HashMap<>();
            if (launcherApps == null) return new ArrayList<>();
            for (UserHandle user : launcherApps.getProfiles()) {
                try {
                    for (LauncherActivityInfo info : launcherApps.getActivityList(null, user)) {
                        final String packageName = info.getComponentName().getPackageName();
                        final int userId = user.getIdentifier();
                        final String key = userId + ":" + packageName;
                        if (!found.containsKey(key)) {
                            found.put(key, new AppEntry(packageName, userId,
                                    info.getLabel().toString(), info));
                        }
                    }
                } catch (SecurityException ignored) {
                    // Ignore locked or inaccessible profiles.
                }
            }
            final List<AppEntry> result = new ArrayList<>(found.values());
            final Collator collator = Collator.getInstance();
            result.sort((left, right) -> {
                int order = collator.compare(left.label, right.label);
                return order != 0 ? order : left.packageName.compareTo(right.packageName);
            });
            return result;
        }

        private static final class AppEntry {
            final String packageName;
            final int userId;
            final String label;
            final LauncherActivityInfo info;
            AppEntry(String packageName, int userId, String label, LauncherActivityInfo info) {
                this.packageName = packageName;
                this.userId = userId;
                this.label = label;
                this.info = info;
            }
        }
    }
}
