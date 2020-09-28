package linushdot.befundpostpcr;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TestDao {

    @Query("SELECT * FROM test ORDER BY date desc")
    LiveData<List<Test>> getAllLive();

    @Query("SELECT * FROM test ORDER BY date desc")
    List<Test> getAll();

    @Query("SELECT * FROM test WHERE result is null ORDER BY date desc")
    List<Test> getPending();

    @Insert
    void insertAll(Test... tests);

    @Update
    void updateAll(Test... tests);

    @Delete
    void delete(Test test);

}
