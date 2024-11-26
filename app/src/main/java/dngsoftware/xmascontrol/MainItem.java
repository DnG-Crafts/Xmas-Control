package dngsoftware.xmascontrol;

import android.graphics.drawable.Drawable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_table")
public class MainItem {


    @SuppressWarnings("UnusedDeclaration")
    @PrimaryKey(autoGenerate = true)
    public int dbkey;

    @SuppressWarnings("UnusedDeclaration")
    @ColumnInfo(name = "device_position")
    public int position;

    @SuppressWarnings("UnusedDeclaration")
    @ColumnInfo(name = "device_name")
    public String deviceName;

    @SuppressWarnings("UnusedDeclaration")
    @ColumnInfo(name = "device_url")
    public String deviceUrl;

    @SuppressWarnings("UnusedDeclaration")
    @ColumnInfo(name = "device_image")
    public String deviceImage;

    @SuppressWarnings("UnusedDeclaration")
    @Ignore
    public Drawable icon;


}