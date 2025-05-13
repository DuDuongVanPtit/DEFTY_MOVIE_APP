package com.example.defty_movie_app.view;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.shared.UserManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PersonalActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "PersonalActivity";
    private ImageView imageViewProfile;
    private EditText editTextNameValue;
    private Spinner spinnerGender;
    private TextView textViewDobValue;
    private Uri pickedImageUri = null;
    private String currentLocalImagePath = null;
    private Calendar calendarInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);

        imageViewProfile = findViewById(R.id.imageViewProfile);
        editTextNameValue = findViewById(R.id.textViewNameValue);
        spinnerGender = findViewById(R.id.spinnerGender);
        textViewDobValue = findViewById(R.id.textViewDobValue);
        TextView personalToolbarSaveButton = findViewById(R.id.personal_toolbar_save_button);

        calendarInstance = Calendar.getInstance();

        loadUserProfile();

        imageViewProfile.setOnClickListener(v -> openImageChooser());
        textViewDobValue.setOnClickListener(v -> showDatePickerDialog());
        personalToolbarSaveButton.setOnClickListener(v -> saveUserProfile());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        Log.d(TAG, "loadUserProfile called");
        String currentName = UserManager.getFullName(this);
        editTextNameValue.setText(currentName);

        final String[] genderArrayFromResources = getResources().getStringArray(R.array.gender_array);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, R.layout.spinner_item_selected_white_text);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_white_text);
        spinnerGender.setAdapter(adapter);

        String savedGender = UserManager.getCurrentGender(this);
        if (savedGender != null && !savedGender.isEmpty()) {
            int spinnerPosition = -1;
            for (int i = 0; i < genderArrayFromResources.length; i++) {
                if (genderArrayFromResources[i].equals(savedGender)) {
                    spinnerPosition = i;
                    break;
                }
            }
            if (spinnerPosition != -1) {
                spinnerGender.setSelection(spinnerPosition);
            } else {
                spinnerGender.setSelection(0);
            }
        } else {
            spinnerGender.setSelection(0);
        }

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView selectedTextView = view instanceof TextView ? (TextView) view : null;
                if (selectedTextView != null) {
                    if (position == 0 && genderOptionsContainsPrompt(genderArrayFromResources)) {
                        selectedTextView.setTextColor(Color.GRAY);
                    } else {
                        selectedTextView.setTextColor(Color.WHITE);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        long dobTimestamp = UserManager.getDateOfBirthTimestamp(this);
        if (dobTimestamp > 0) {
            calendarInstance.setTimeInMillis(dobTimestamp);
            updateDobLabel();
        } else {
            textViewDobValue.setText("");
            textViewDobValue.setHint("Nhấn để chọn ngày");
            textViewDobValue.setHintTextColor(Color.GRAY);
        }

        currentLocalImagePath = UserManager.getProfileImagePath(this);
        Log.d(TAG, "Loading image from path: " + currentLocalImagePath);
        if (currentLocalImagePath != null && !currentLocalImagePath.isEmpty()) {
            File imageFile = new File(currentLocalImagePath);
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .circleCrop()
                        .into(imageViewProfile);
            } else {
                Log.w(TAG, "Image file not found at path: " + currentLocalImagePath);
                imageViewProfile.setImageResource(R.drawable.ic_avatar);
            }
        } else {
            imageViewProfile.setImageResource(R.drawable.ic_avatar);
        }
        pickedImageUri = null;
    }

    private boolean genderOptionsContainsPrompt(String[] genderArray) {
        if (genderArray.length > 0) {
            String firstItem = genderArray[0].toLowerCase();
            return firstItem.contains("chọn") || firstItem.contains("select");
        }
        return false;
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            pickedImageUri = data.getData();
            Log.d(TAG, "Image picked: " + pickedImageUri.toString());
            Glide.with(this)
                    .load(pickedImageUri)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(imageViewProfile);
        } else {
            Log.d(TAG, "Image picking cancelled or failed.");
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendarInstance.set(Calendar.YEAR, year);
            calendarInstance.set(Calendar.MONTH, monthOfYear);
            calendarInstance.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDobLabel();
        };

        new DatePickerDialog(PersonalActivity.this, dateSetListener,
                calendarInstance.get(Calendar.YEAR),
                calendarInstance.get(Calendar.MONTH),
                calendarInstance.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDobLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        textViewDobValue.setText(sdf.format(calendarInstance.getTime()));
        textViewDobValue.setTextColor(Color.WHITE);
        textViewDobValue.setHint("");
    }

    private void saveUserProfile() {
        Log.d(TAG, "saveUserProfile called");
        String newName = editTextNameValue.getText().toString().trim();
        String selectedGender = "";
        long newDobTimestamp = 0L;

        final String[] genderArrayFromResources = getResources().getStringArray(R.array.gender_array);
        if (spinnerGender.getSelectedItemPosition() >= 0) {
            String potentialGender = spinnerGender.getSelectedItem().toString();
            if (!(spinnerGender.getSelectedItemPosition() == 0 && genderOptionsContainsPrompt(genderArrayFromResources))) {
                selectedGender = potentialGender;
            }
        }

        if (!TextUtils.isEmpty(textViewDobValue.getText()) &&
                !textViewDobValue.getText().toString().equals(textViewDobValue.getHint() != null ? textViewDobValue.getHint().toString() : "")) {
            newDobTimestamp = calendarInstance.getTimeInMillis();
        }

        if (newName.isEmpty()) {
            editTextNameValue.setError("Tên không được để trống");
            editTextNameValue.requestFocus();
            return;
        }

        boolean nameUpdated = UserManager.updateFullName(this, newName);
        boolean genderUpdated = UserManager.updateGender(this, selectedGender);
        boolean dobUpdated = UserManager.updateDateOfBirth(this, newDobTimestamp);

        boolean imageHandledSuccessfully = false;
        boolean newImageWasPicked = (pickedImageUri != null);

        if (newImageWasPicked) {
            Log.d(TAG, "New image was picked, attempting to save to internal storage.");
            String newLocalPath = saveImageToInternalStorage(pickedImageUri);
            if (newLocalPath != null) {
                Log.d(TAG, "New image saved to internal storage: " + newLocalPath);
                if (currentLocalImagePath != null && !currentLocalImagePath.equals(newLocalPath)) {
                    File oldImageFile = new File(currentLocalImagePath);
                    if (oldImageFile.exists()) {
                        if (oldImageFile.delete()) {
                            Log.d(TAG, "Old image deleted: " + currentLocalImagePath);
                        } else {
                            Log.w(TAG, "Failed to delete old image: " + currentLocalImagePath);
                        }
                    }
                }
                if (UserManager.updateProfileImagePath(this, newLocalPath)) {
                    currentLocalImagePath = newLocalPath;
                    imageHandledSuccessfully = true;
                    Log.d(TAG, "New image path saved to UserManager.");
                } else {
                    Log.e(TAG, "Failed to save new image path to UserManager.");
                }
            } else {
                Log.e(TAG, "Failed to save new image to internal storage.");
            }
        } else {
            imageHandledSuccessfully = true;
        }

        if (nameUpdated || genderUpdated || dobUpdated || (newImageWasPicked && imageHandledSuccessfully)) {
            Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
            if (newImageWasPicked && !imageHandledSuccessfully) {
                Toast.makeText(this, "Lưu thông tin chữ thành công, nhưng lỗi cập nhật ảnh đại diện.", Toast.LENGTH_LONG).show();
            }

        } else if (newImageWasPicked) {
            Toast.makeText(this, "Lỗi cập nhật ảnh đại diện. Các thông tin khác không thay đổi.", Toast.LENGTH_LONG).show();
        } else {
            // Toast.makeText(this, "Không có thông tin nào thay đổi.", Toast.LENGTH_SHORT).show();
        }
        pickedImageUri = null;
         finish();
    }

    private String saveImageToInternalStorage(Uri uri) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = this.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Cannot open InputStream for URI: " + uri);
                return null;
            }

            File directory = this.getFilesDir();
            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            File internalFile = new File(directory, fileName);

            outputStream = Files.newOutputStream(internalFile.toPath());
            byte[] buffer = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.i(TAG, "Image successfully saved to: " + internalFile.getAbsolutePath());
            return internalFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image to internal storage from URI: " + uri, e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams: ", e);
            }
        }
    }
}