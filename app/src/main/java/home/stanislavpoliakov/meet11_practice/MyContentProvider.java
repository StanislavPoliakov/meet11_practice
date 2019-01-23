package home.stanislavpoliakov.meet11_practice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class MyContentProvider extends ContentProvider {
    private static final String TAG = "meet11_logs";
    private static final String AUTHORITY = "content_provider";
    private static final String ENTRIES_TABLE = "new_database";
    private static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE);

    private static final int ENTRIES = 100;
    private static final int ENTRY_ID = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, ENTRIES_TABLE, ENTRIES);
        uriMatcher.addURI(AUTHORITY, ENTRIES_TABLE + "/#", ENTRY_ID);
    }

    private DatabaseManager mDatabase;

    public MyContentProvider() {
    }

    @Override
    public boolean onCreate() {
        mDatabase = DatabaseManager.getInstance(getContext().getApplicationContext());
        return mDatabase != null;
    }

    /**
     * Метод получения всех элементов базы в виде объекта Cursor. Реализация: Тип возвращаемого значения
     * метода DAO - Cursor. Реализация в DatabaseManager - получаем cursor, конвертируем в List<Entry>
     * и на отрисовку в RecyclerView отправляется уже список элементов. Но здесь светит Cursor
     * @param uri фактически, это адрес всей таблицы
     * @param projection не реализовано здесь
     * @param selection не реализовано здесь
     * @param selectionArgs не реализовано здесь
     * @param sortOrder не реализовано здесь
     * @return объект Cursor на список найденных значений
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int uriType = uriMatcher.match(uri);
        Cursor cursor;
        if (uriType == ENTRIES) {
            cursor = mDatabase.readEntriesAll();
        }
        else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Метод вставки элемента базы данных на уровне ContentProvider
     * @param uri адрес (ссылка), куда будем вставлять элемент
     * @param values данные записи (Entry), преобразованные к ContentValues через конвертер
     * @return обновленный URI, по которому теперь распологается элемент базы
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        long id;
        if (uriType == ENTRY_ID) {
            id = mDatabase.insertEntry(ConvertUtils.convertValuesToEntry(values));
            Log.d(TAG, "insert: id = " + id);
        } else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.parse(CONTENT_URI + "/" + id);
    }

    /**
     * Метод обновления элемента базы данных на уровне ContentProvider
     * @param uri адрес (ссылка) изменяемого элемента
     * @param values новые данные для изменения
     * @param selection WHERE - не используется здесь (null)
     * @param selectionArgs WHERE params - не используется здесь (null)
     * @return количество заменнеых элементов
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        int rowsUpdated;
        if (uriType == ENTRY_ID) {
            rowsUpdated = mDatabase.updateEntry(ConvertUtils.convertValuesToEntry(values));
        }
        else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }

    /**
     * Метод удаления элемента из базы данных на уровне ContentProvider
     * @param uri адрес (ссылка) элемента, который необходимо удалить
     * @param selection WHERE - реализовано на уровне Data Access Object
     * @param selectionArgs WHERE params - аргументы выбора не реализованы здесь
     * @return количество удаленных элементов
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        int rowsDeleted;
        if (uriType == ENTRY_ID) {
            String stringID = uri.getLastPathSegment();
            int id = Integer.parseInt(stringID);
            //Log.d(TAG, "delete: ID = " + id);
            rowsDeleted = mDatabase.deleteEntryById(id);
            //Log.d(TAG, "delete: Rows Deleted = " + rowsDeleted);
        }
        else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        Log.d(TAG, "delete: URI = " + uri);
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
