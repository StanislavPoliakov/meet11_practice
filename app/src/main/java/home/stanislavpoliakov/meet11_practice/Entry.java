package home.stanislavpoliakov.meet11_practice;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * Класс записи, в котором:
 * timeStamp - дата и вермя создания записи
 * title - название записи
 * text - текст записи
 */
@Entity (tableName = "entries", indices = {@Index("id")})
public class Entry implements Cloneable{
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;

    @ColumnInfo(name = "entry_text")
    private String text;

    @ColumnInfo(name = "timestamp")
    private String timeStamp;

    @Ignore
    private boolean isLarge;

    @Ignore
    private static final String TAG = "meet9_logs";

    @Ignore
    public Entry(String title, String text) {
        this.title = title;
        this.text = text;

        //TODO Релизовать сохранение записи в отдельный файл, если запись слишком большая
        //TODO Очевидно, что в база должна хранить ссылку на файл или NULL, если запись сохранена в базе
        isLarge = false;
    }

    public Entry(String title, String text, int id) {
        this.title = title;
        this.text = text;
        this.id = id;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;

    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public String getTitle() {
        return this.title;
    }

    public String getText() {
        return this.text;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Ignore
    public boolean isLarge() {
        return this.isLarge;
    }

    /**
     * Переопределен, но не использован
     * @return deep-copy
     * @throws CloneNotSupportedException
     */
    @Ignore
    @Override
    protected Object clone() throws CloneNotSupportedException {
        Entry clonedEntry = (Entry) super.clone();
        clonedEntry.title = this.title;
        clonedEntry.text = this.text;
        clonedEntry.id = this.id;
        clonedEntry.timeStamp = this.timeStamp;
        return clonedEntry;
    }
}
