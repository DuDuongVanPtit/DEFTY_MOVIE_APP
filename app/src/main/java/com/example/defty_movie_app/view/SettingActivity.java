package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.SettingsAdapter;
import com.example.defty_movie_app.data.dto.SettingItem;
import com.example.defty_movie_app.utils.LocaleHelper;
import com.example.defty_movie_app.viewmodel.SettingsViewModel;

public class SettingActivity extends AppCompatActivity {

    private SettingsViewModel viewModel;
    private SettingsAdapter adapter;
    private String[] listPreferenceEntryValues;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en");
        Context context = LocaleHelper.wrap(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.settings_recycler_view); // ID từ layout của bạn
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        adapter = new SettingsAdapter(new SettingsAdapter.SettingItemDiffCallback(), item -> {
            viewModel.handleSettingClick(item);
        });
        recyclerView.setAdapter(adapter);

        AppCompatButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            viewModel.logout();
            Toast.makeText(this, getString(R.string.logout_successful), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        viewModel.getSettingsListLiveData().observe(this, settingItems -> {
            adapter.submitList(settingItems);
        });

        viewModel.getOpenListPreferenceDialogEvent().observe(this, settingItem -> {
            if (settingItem != null) {
                showListPreferenceDialog(settingItem);
                viewModel.onListPreferenceDialogOpened();
            }
        });

        viewModel.getPerformActionEvent().observe(this, actionKey -> {
            if (actionKey != null) {
                handleAction(actionKey);
                viewModel.onActionPerformed();
            }
        });
    }

    private void showListPreferenceDialog(SettingItem item) {
        String[] listPreferenceEntries;
        if ("display_mode".equals(item.getKey())) {
            listPreferenceEntries = getResources().getStringArray(R.array.pref_display_mode_entries);
            listPreferenceEntryValues = getResources().getStringArray(R.array.pref_display_mode_values);
        } else {
            return;
        }

        String currentValue = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(item.getKey(), listPreferenceEntryValues[0]);
        int checkedItem = -1;
        for (int i = 0; i < listPreferenceEntryValues.length; i++) {
            if (listPreferenceEntryValues[i].equals(currentValue)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setSingleChoiceItems(listPreferenceEntries, checkedItem, (dialog, which) -> {
                    viewModel.updateListPreferenceValue(item.getKey(), listPreferenceEntryValues[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleAction(String actionKey) {
        switch (actionKey) {
            case "privacy_policy":
                Toast.makeText(this, "Mở Chính sách bảo mật...", Toast.LENGTH_SHORT).show();
                break;
            case "terms_of_service":
                Toast.makeText(this, "Mở Điều khoản dịch vụ...", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
}