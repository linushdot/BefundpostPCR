package linushdot.befundpostpcr;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

@Entity
public class Test implements Cloneable {
    @PrimaryKey
    public Integer id;
    @TypeConverters(Converters.class)
    public Date date;
    public String code;
    public String result;
    @TypeConverters(Converters.class)
    public Date resultDateTime;
    public boolean toBeDeleted;

    public Test copy() {
        try {
            return (Test) super.clone();
        } catch(CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test test = (Test) o;
        return Objects.equals(id, test.id) &&
                Objects.equals(date, test.date) &&
                Objects.equals(code, test.code) &&
                Objects.equals(result, test.result) &&
                Objects.equals(resultDateTime, test.resultDateTime) &&
                Objects.equals(toBeDeleted, test.toBeDeleted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, code, result);
    }

    public String getName() {
        return getDateFormat().format(date) + " " + code;
    }

    public String getResultInfo() {
        if(result != null && resultDateTime != null) {
            return result + " (" + getDateTimeFormat().format(resultDateTime) + ")";
        }
        return result;
    }

    protected DateFormat getDateFormat() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    protected DateFormat getDateTimeFormat() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    }
}
