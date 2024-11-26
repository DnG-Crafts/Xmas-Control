package dngsoftware.xmascontrol;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDB {

    @SuppressWarnings("UnusedDeclaration")
    @Insert
    void addItem(MainItem item);

    @SuppressWarnings("UnusedDeclaration")
    @Update
    void updateItem(MainItem item);

    @SuppressWarnings("UnusedDeclaration")
    @Query("UPDATE app_table SET device_position =:pos WHERE device_url =:deviceUrl")
    void updatePosition(int pos, String deviceUrl);

    @SuppressWarnings("UnusedDeclaration")
    @Delete
    void deleteItem(MainItem item);

    @SuppressWarnings("UnusedDeclaration")
    @Query("SELECT COUNT(dbkey) FROM app_table")
    int getItemCount();

    @SuppressWarnings("UnusedDeclaration")
    @Query("SELECT * FROM app_table ORDER BY device_position ASC")
    List<MainItem> getAllitems();

    @SuppressWarnings("UnusedDeclaration")
    @Query("DELETE FROM app_table")
    void deleteAll();

    @SuppressWarnings("UnusedDeclaration")
    @Query("SELECT device_url FROM app_table WHERE device_url = :deviceUrl")
    String getitem(String deviceUrl);

    @SuppressWarnings("UnusedDeclaration")
    @Query("SELECT * FROM app_table WHERE device_url = :deviceUrl")
    MainItem getMainitem(String deviceUrl);
}
