package home.stanislavpoliakov.meet11_practice;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.database.Cursor;

import java.util.List;

@Dao
public interface EntryDAO {

    /**
     * Метод, возвращающий из базы список записей в формате List<Entry>
     * В текущей версии не используется
     * @return
     */
    @Query("SELECT * FROM entries")
    List<Entry> getEntries();

    /**
     * Метод, возвращающий из базы список записей в формате объекта Cursor.
     * Дальнейшие преобразования описаны в ConvertUtils
     * @return объект Cursor (найденные значения)
     */
    @Query("SELECT * FROM entries")
    Cursor getEntriesAll();

    /**
     * Метод удаления записи по id
     * Room предлагает только примитивное удаление элемента базы данных (@Delete),
     * то есть удалелние сущности, которую необходимо передать в качестве аргумента.
     * В нашем случае, необходимо удалить запись по id, поэтому реализация метода удаления
     * представлена в виде Query
     * @param id по которому нужно найти элемент в базе данных для последующего удаления
     * @return количество удаленных записей
     */
    @Query("DELETE FROM entries WHERE id = :id")
    int deleteEntryById(int id);

    /**
     * Метод добавления записи в базу данных
     * @param entry запись, которую небходимо добавить
     * @return id добавленной записи, который формирует база данных (primary key, autoincrement)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertEntry(Entry entry);

    /**
     * Метод обноления записи в базе данных. Фактически, метод замены записей
     * @param entry запись, которую необходимо обновить
     * @return количество обновленных записей
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateEntry(Entry entry);

    // Вот этот "красавец", который удаляет запись только по сущности, переданной в качестве аргумента ))
    // В текущей версии не используется. Оставил для напоминания
    @Delete
    int deleteEntry(Entry entry);
}
