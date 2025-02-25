package com.arpit.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private boolean doubleBackToExitPressedOnce = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private LocationManager locationManager;
    private double latitude = 0.0, longitude = 0.0;
    private final Handler handler = new Handler();
    private final Runnable resetDoubleBackFlag = () -> doubleBackToExitPressedOnce = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);

        ImageButton btnAddInfo = findViewById(R.id.btnAddInfo); // Reference button
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // Request location updates
        requestLocationUpdates();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, true);
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            loadMap();
        }
        btnAddInfo.setOnClickListener(v -> {
            // Pass the location to AddInfoActivity
            Intent intent = new Intent(MainActivity.this, AddInfoActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startActivity(intent);
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(resizeLogo());
        }
    }

    private void loadMap() {
        webView.loadUrl("file:///android_asset/Map.html");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // Perform logout operation here
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

        handler.postDelayed(resetDoubleBackFlag, 2000); // Reset the flag after 2 seconds
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(resetDoubleBackFlag); // Clean up the handler when activity is destroyed
    }

    private Drawable resizeLogo() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.logo0);
        if (drawable != null) {
            drawable.setBounds(0, 0, 0, 0);
        }
        return drawable;
    }
    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMap();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage("Location permission is required to show the map.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }
}
