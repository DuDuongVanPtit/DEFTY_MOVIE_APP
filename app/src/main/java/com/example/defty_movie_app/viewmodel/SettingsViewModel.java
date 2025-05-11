package com.example.defty_movie_app.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.SettingItem;
import com.example.defty_movie_app.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.List;


public class SettingsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<SettingItem>> settingsListLiveData = new MutableLiveData<>();
    private final SharedPreferences sharedPreferences;
    private final Resources resources;

    // LiveData để kích hoạt các sự kiện điều hướng hoặc dialog từ Activity/Fragment
    private final MutableLiveData<SettingItem> openListPreferenceDialogEvent = new MutableLiveData<>();
    private final MutableLiveData<String> performActionEvent = new MutableLiveData<>();

    public SettingsViewModel(Application application) {
        super(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        resources = application.getResources();
        loadSettings();
    }

    public LiveData<List<SettingItem>> getSettingsListLiveData() {
        return settingsListLiveData;
    }

    public LiveData<SettingItem> getOpenListPreferenceDialogEvent() {
        return openListPreferenceDialogEvent;
    }

    public LiveData<String> getPerformActionEvent() {
        return performActionEvent;
    }

    // Gọi sau khi sự kiện đã được xử lý để tránh kích hoạt lại
    public void onListPreferenceDialogOpened() {
        openListPreferenceDialogEvent.setValue(null);
    }
    public void onActionPerformed() {
        performActionEvent.setValue(null);
    }

    private void loadSettings() {
        List<SettingItem> items = new ArrayList<>();

        String displayModeValue = sharedPreferences.getString("display_mode", "system");
        String displayModeSummary = getEntryForValue(
                displayModeValue,
                resources.getStringArray(R.array.pref_display_mode_values),
                resources.getStringArray(R.array.pref_display_mode_entries)
        );
        items.add(new SettingItem(
                "display_mode",
                resources.getString(R.string.pref_display_mode_title), // Cần tạo string này
                null, // Summary chung có thể không cần nếu currentValue hiển thị đủ
                displayModeSummary,
                true,
                SettingItem.SettingItemType.LIST_PREFERENCE
        ));

        // 3. App Version (InfoPreference)
        items.add(new SettingItem(
                "app_version",
                resources.getString(R.string.pref_app_version_title),
                null,
                "1.0.0",
                false,
                SettingItem.SettingItemType.INFO_PREFERENCE
        ));

        // 4. Privacy Policy
        items.add(new SettingItem(
                "privacy_policy",
                resources.getString(R.string.pref_privacy_policy_title),
                null,
                null,
                true,
                SettingItem.SettingItemType.ACTION_PREFERENCE
        ));

        // 5. Terms of Service
        items.add(new SettingItem(
                "terms_of_service",
                resources.getString(R.string.pref_terms_of_service_title),
                null, null, true,
                SettingItem.SettingItemType.ACTION_PREFERENCE
        ));

        settingsListLiveData.setValue(items);
    }

    private String getEntryForValue(String value, String[] entryValues, String[] entries) {
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(value)) {
                return entries[i];
            }
        }
        return entries.length > 0 ? entries[0] : value;
    }

    public void handleSettingClick(SettingItem item) {
        if (!item.isSelectable()) {
            return;
        }
        switch (item.getType()) {
            case LIST_PREFERENCE:
                openListPreferenceDialogEvent.setValue(item);
                break;
            case ACTION_PREFERENCE:
                performActionEvent.setValue(item.getKey());
                break;
            case INFO_PREFERENCE:
                break;
        }
    }

    public void updateListPreferenceValue(String key, String newValue) {
        sharedPreferences.edit().putString(key, newValue).apply();
        loadSettings();
    }
}
