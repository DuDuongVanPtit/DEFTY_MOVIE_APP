package com.example.defty_movie_app.view;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.example.defty_movie_app.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        ListPreference displayModePreference = findPreference("display_mode");
        if (displayModePreference != null) {
            displayModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // TODO: Thêm logic để thay đổi theme ứng dụng dựa trên newValue (light, dark, system)
                Toast.makeText(getContext(), "Chế độ hiển thị đã đổi thành: " + newValue, Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference logoutPreference = findPreference("logout");
        if (logoutPreference != null) {
            logoutPreference.setOnPreferenceClickListener(preference -> {
                // TODO: Thêm logic đăng xuất ở đây
                Toast.makeText(getContext(), "Đã nhấn Đăng xuất!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference appVersionPreference = findPreference("app_version");
        if (appVersionPreference != null) {
            try {
                if (getActivity() != null && getActivity().getPackageManager() != null && getActivity().getPackageName() != null) {
                    PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                    String version = pInfo.versionName;
                    appVersionPreference.setSummary(version);
                } else {
                    appVersionPreference.setSummary("N/A");
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                appVersionPreference.setSummary("N/A");
            }
        }

        Preference privacyPolicyPreference = findPreference("privacy_policy");
        if (privacyPolicyPreference != null) {
            privacyPolicyPreference.setOnPreferenceClickListener(preference -> {
                // TODO: Thay thế bằng URL chính sách bảo mật của bạn
                String url = "https://your-privacy-policy-url.com";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không thể mở liên kết", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference termsOfServicePreference = findPreference("terms_of_service");
        if (termsOfServicePreference != null) {
            termsOfServicePreference.setOnPreferenceClickListener(preference -> {
                // TODO: Thay thế bằng URL điều khoản dịch vụ của bạn
                String url = "https://your-terms-of-service-url.com";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không thể mở liên kết", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }
}