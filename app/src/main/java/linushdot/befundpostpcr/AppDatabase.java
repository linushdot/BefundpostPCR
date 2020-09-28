package linushdot.befundpostpcr;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {Test.class},
        version = 6
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NAME = "app-db";

    private static AppDatabase INSTANCE;
    private static final Object sLock = new Object();

    public static AppDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, AppDatabase.class, NAME)
                        .fallbackToDestructiveMigration()
                        .build();
            }
            return INSTANCE;
        }
    }

    public abstract TestDao testDao();

}
