package com.arpit.project;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CropInfo.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract CropInfoDao cropInfoDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "crop_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // Avoid for large data
                    .build();
        }
        return instance;
    }
}

