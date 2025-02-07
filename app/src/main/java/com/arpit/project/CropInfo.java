package com.arpit.project;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crop_info")
public class CropInfo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public double latitude;
    public double longitude;
    public String crop;
    public String cropStage;
    public String photo;
    public String dateTime;

    public CropInfo(double latitude, double longitude, String crop, String cropStage, String photo, String dateTime) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.crop = crop;
        this.cropStage = cropStage;
        this.photo = photo;
        this.dateTime = dateTime;
    }
}

