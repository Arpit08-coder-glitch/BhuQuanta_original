package com.arpit.project;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CropInfoDao {
    @Insert
    void insert(CropInfo cropInfo);

    @Query("SELECT * FROM crop_info")
    List<CropInfo> getAllCropInfo();

    @Query("DELETE FROM crop_info WHERE id = :id")
    void deleteById(int id);
}
