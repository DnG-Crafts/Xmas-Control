package dngsoftware.xmascontrol;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MainItem.class}, version = 2, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase {
    public abstract AppDB appDB();
}


