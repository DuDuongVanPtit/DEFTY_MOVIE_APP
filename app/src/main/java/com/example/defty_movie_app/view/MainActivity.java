package com.example.defty_movie_app.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_lang", "en"); // Default to English if no preference
        Context context = LocaleHelper.wrap(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home); // Default to Home
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_explore) { // Assuming nav_explore is your Library
            selectedFragment = new LibraryFragment();
        } else if (itemId == R.id.nav_download) { // NEW: Handle Download Tab
            selectedFragment = new DownloadFragment();
        } else if (itemId == R.id.nav_me) {
            selectedFragment = new ProfileFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        // Check if the fragment is already added to prevent overlapping or errors
        // if (getSupportFragmentManager().findFragmentById(R.id.contentLayout) == fragment) {
        //     return;
        // }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.contentLayout, fragment); // Use contentLayout from your activity_main.xml
        // transaction.addToBackStack(null); // Optional: if you want back navigation for fragments
        transaction.commit();
    }
}
