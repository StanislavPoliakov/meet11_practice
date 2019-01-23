package home.stanislavpoliakov.meet11_practice;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для конвертации форматов. Содержит статические методы. Ненаследуемый
 */
public final class ConvertUtils {
    private static final String TITLE = "title";
    private static final String TEXT = "entry_text";
    private static final String ID = "id";

    /**
     * Метод преобразования форматов. Используется для создания (insert) и обновления (update)
     * записей в базе данных на участке взаимодействия ContentProvider -> Database
     * @param contentValues значения в формате ContentValues
     * @return объект записи Entry
     */
    public static Entry convertValuesToEntry(ContentValues contentValues) {
        Entry entry = new Entry(contentValues.getAsString(TITLE),
                contentValues.getAsString(TEXT),
                contentValues.getAsInteger(ID));
        return entry;
    }

    /**
     * Метод преобразования форматов. Используется для создания (insert) и обновления (update)
     * записей в базе данных на участке взаимодействия Activity -> ContentProvider
     * @param entry объект записи Entry
     * @return значения в формате ContentValues
     */
    public static ContentValues convertEntryToValues(Entry entry) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(TITLE, entry.getTitle());
        contentValues.put(TEXT, entry.getText());
        contentValues.put(ID, entry.getId());

        return contentValues;
    }

    /**
     * Метод преборазования форматов. Используется для преборазования полученных данных из
     * базы данных (SELECT * FROM entries) в список записей, который мы отправляем в Handler, а
     * затем в Activity при инициализации RecyclerView, а затем при отрисовке RecyclerView
     * @param cursor объект Cursor со списком найденных элементов базы данных
     * @return список записей в формате List<Entry>
     */
    public static List<Entry> convertCursorToEntryList(Cursor cursor) {
        List<Entry> entryList = new ArrayList<>();

        // Перемещаем значение курсора в начало
        cursor.moveToFirst();

        // Пока курсор не указывает "за последний элемент", то есть пока есть элементы...
        while (!cursor.isAfterLast()) {

            // Получаем поля записи базы данных
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String text = cursor.getString(cursor.getColumnIndex("entry_text"));
            int id = cursor.getInt(cursor.getColumnIndex("id"));

            // Создаем новый объект Entry по считанным значениям и добавляем в список записей
            Entry entry = new Entry(title, text, id);
            entryList.add(entry);

            // Передвигаем курсор к следующей записи найденного списка
            cursor.moveToNext();
        }
        cursor.close();
        return entryList;
    }
}
