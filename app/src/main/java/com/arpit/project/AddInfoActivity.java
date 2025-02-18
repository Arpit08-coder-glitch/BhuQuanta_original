package com.arpit.project;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.location.FusedLocationProviderClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.TextView;
import java.util.List;
import java.util.Objects;

public class AddInfoActivity extends AppCompatActivity {
    private EditText etCrop, etCropStage;
    private ImageView imagePreview;
    private String imageBase64 = "";
    private TextView tvDateTime;

    private static final int PICK_IMAGE_REQUEST = 1;

    private double latitude, longitude;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info);
        etCrop = findViewById(R.id.etCrop);
        etCropStage = findViewById(R.id.etCropStage);
        imagePreview = findViewById(R.id.imagePreview);
        Button btnUploadImage = findViewById(R.id.btnUploadImage);
        Button btnSubmit = findViewById(R.id.btnSubmit);
        // Find the TextView where you want to show the date and time
        tvDateTime = findViewById(R.id.tvDateTime);
        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        // Set the current date and time to the TextView
        tvDateTime.setText(currentDateAndTime);
        syncLocalData();
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Get current location
        getCurrentLocation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 100);
            }
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(resizeLogo());
        }
        // Get coordinates from MainActivity
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Handle image upload
        btnUploadImage.setOnClickListener(v -> showImagePickerDialog());
        // Handle form submission
        btnSubmit.setOnClickListener(v -> saveDataToFirestore());
    }
    private Drawable resizeLogo() {
        @SuppressLint("ResourceType") Drawable drawable = ContextCompat.getDrawable(this, 2131165402);
        if (drawable != null) {
            drawable.setBounds(0, 0, 0, 0);
        }
        return drawable;
    }
    private void getCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        } else {
                            Toast.makeText(AddInfoActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Permission to access location denied", Toast.LENGTH_SHORT).show();
        }
    }
    // Show image picker dialog (Camera or Gallery)
    @SuppressLint("IntentReset")
    private void showImagePickerDialog() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent chooser = Intent.createChooser(intent, "Select Image");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        startActivityForResult(chooser, PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                // Gallery Image
                Uri imageUri = data.getData();
                convertImageToBase64(imageUri);
                imagePreview.setImageURI(imageUri); // Update ImageView
                imagePreview.setVisibility(View.VISIBLE); // Show the image preview
            } else {
                // Camera Image
                assert data != null;
                Bitmap photo = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                if (photo != null) {
                    convertBitmapToBase64(photo);
                    imagePreview.setImageBitmap(photo); // Update ImageView
                    imagePreview.setVisibility(View.VISIBLE); // Show the image preview
                } else {
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    // Convert image URI to Base64
    private void convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                convertBitmapToBase64(bitmap);
                inputStream.close();
            } else {
                Toast.makeText(this, "Error: Unable to open image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
        imageBase64 = "data:image/png;base64," + imageBase64;
    }
    // Store data in Firestore
    private void saveDataToFirestore() {
        String crop = etCrop.getText().toString().trim();
        String cropStage = etCropStage.getText().toString().trim();
        String DateandTime = tvDateTime.getText().toString().trim();

        if (crop.isEmpty() || cropStage.isEmpty() || imageBase64.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and upload an image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isInternetAvailable()) {
            // Upload to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("Latitude", latitude);
            data.put("Longitude", longitude);
            data.put("Crop", crop);
            data.put("Crop Stage", cropStage);
            data.put("Photo", imageBase64);
            data.put("Date & Time", DateandTime);

            db.collection("crop_data").add(data)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AddInfoActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show());
        } else {
            // Store in local database
            AppDatabase database = AppDatabase.getInstance(this);
            CropInfoDao cropInfoDao = database.cropInfoDao();
            CropInfo cropInfo = new CropInfo(latitude, longitude, crop, cropStage, imageBase64, DateandTime);
            cropInfoDao.insert(cropInfo);
            Toast.makeText(this, "No Internet! Data saved locally.", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    private void syncLocalData() {
        if (isInternetAvailable()) {
            AppDatabase database = AppDatabase.getInstance(this);
            CropInfoDao cropInfoDao = database.cropInfoDao();
            List<CropInfo> localData = cropInfoDao.getAllCropInfo();

            if (!localData.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                for (CropInfo data : localData) {
                    Map<String, Object> firestoreData = new HashMap<>();
                    firestoreData.put("Latitude", data.latitude);
                    firestoreData.put("Longitude", data.longitude);
                    firestoreData.put("Crop", data.crop);
                    firestoreData.put("Crop Stage", data.cropStage);
                    firestoreData.put("Photo", data.photo);
                    firestoreData.put("Date & Time", data.dateTime);

                    db.collection("crop_data").add(firestoreData)
                            .addOnSuccessListener(documentReference -> {
                                // Delete from local database after successful sync
                                cropInfoDao.deleteById(data.id);
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to sync data", Toast.LENGTH_SHORT).show());
                }
                Toast.makeText(this, "Local data synced to Firestore!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
