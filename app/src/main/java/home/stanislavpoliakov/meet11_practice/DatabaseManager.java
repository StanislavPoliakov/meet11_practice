package home.stanislavpoliakov.meet11_practice;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс, отвечающий за взаимодействие с базой данных. Singleton
 * Конекртные реализации методов взаимодействия описаны в EntryDAO
 */
public class DatabaseManager {
    // Идентификаторы сообщений msg.what
    public static final int DATABASE_ENTRIES = 1;
    public static final int REPAINT_REQUEST = 2;

    private static final String TAG = "meet11_logs";
    private static DatabaseManager instance;
    private EntryDAO dao;
    private ExecutorService pool;
    private Handler mHandler = new Handler();

    /**
     *  Приватный конструтор.
     *  database = база данных в формате Room
     *  dao = Data Access Object
     *  pool = ThreadPool из одного потока (Executor). Поскольку логика программы не подразумевает
     *  параллельное (между собой) выполнение background потоков, а лишь требует последовательного
     *  выполнения "затратных" по времени задач, то выбран Single Thread Executor. Нам более одного
     *  потока (помимо UI-Thread, разумеется) и не требуется, а значит мы не расходуем память на
     *  лишние ThreadLocal переменные и не расходуем процессорное время на context switch между потоками
     * @param context вызывающий контекст
     */
    private DatabaseManager(Context context) {
        EntryDatabase database = Room.databaseBuilder(context.getApplicationContext(),
                EntryDatabase.class, "new_database")
                .fallbackToDestructiveMigration()
                .build();
        this.dao = database.getEntryDAO();
        this.pool = Executors.newSingleThreadExecutor();
    }

    public static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    /**
     * Метод чтения записей из базы данных (SELECT * FROM entries)
     * @return объект Cursor = найденные значения в базе даных
     */
    public Cursor readEntriesAll() {
        try {
            // Разбил цепочку композиции CompletableFurure, потому что нужно в середине цепочки
            // вернуть результат (по готовности)

            // Получаем записи из базы данных в отдельном потоке
            CompletableFuture<Cursor> completableFuture = CompletableFuture
                    .supplyAsync(() -> dao.getEntriesAll(), pool);

            // Формируем результат по готовности
            Cursor result = completableFuture.get();

            // После того, как предыдущее действие успешно выполнено, запускаем следующие:
            // 1. Конвертируем полученные данные в спиок записей (Cursor -> List<Entry), отдельный поток
            // 2. Передаем результат в Activity, отдельный поток
            completableFuture.thenApplyAsync(ConvertUtils::convertCursorToEntryList, pool)
                    .thenAcceptAsync(this::postResult, pool);

            return result;

        } catch (ExecutionException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Метод добавления записи в базу данных
     * @param entry запись в формате Entry, которую необходимо добавить
     * @return id записи, который сформировала база данных (primary key, autoincrement)
     */
    public long insertEntry(Entry entry) {
        try {
            // Вставляем запись в базу данных и получаем id по готовности, отдельный поток
            CompletableFuture<Long> completableFuture = CompletableFuture
                    .supplyAsync(() -> dao.insertEntry(entry), pool);

            // Формируем результат
            long result = completableFuture.get();

            // Сообщаем в Activity, что были внесены изменения в базе данных, чтобы та перерисовала
            // RecyclerView. Запущено в отдельном потоке, хоть и не обязательно
            completableFuture.thenRunAsync(this::postRepaint, pool);

            return result;

        } catch (ExecutionException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    /**
     * Метод обновления записи в базе данных
     * @param entry запись в формате Entry, которую необходимо обновить
     * @return количество обновленных записей за итерацию (в нашем случае всегда 1)
     */
    public int updateEntry(Entry entry) {
        try {
            // Обновляем запись в базе данных (REPLACE) и получаем количесто обновлений, отдельный поток
            CompletableFuture<Integer> completableFuture = CompletableFuture
                    .supplyAsync(() -> dao.updateEntry(entry), pool);

            // Формируем результат
            int result = completableFuture.get();

            // Сообщаем в Activity, что были внесены изменения в базе данных, чтобы та перерисовала
            // RecyclerView. Запущено в отдельном потоке, хоть и не обязательно
            completableFuture.thenRunAsync(this::postRepaint, pool);

            return result;

        } catch (ExecutionException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return 0;

    }

    /**
     * Метод удаления записи из базы данных по ID.
     * @param id записи, которую необходимо удалить
     * @return количество удаленных записей за итерацию (в нашем случае всегда 1)
     */
    public int deleteEntryById(int id) {
        try {
            // Удаляем запись из базы данных, отедльный поток
            CompletableFuture<Integer> completableFuture = CompletableFuture
                    .supplyAsync(() -> dao.deleteEntryById(id), pool);

            // Формируем результат
            int result = completableFuture.get();

            // Сообщаем в Activity, что были внесены изменения в базе данных, чтобы та перерисовала
            // RecyclerView. Запущено в отдельном потоке, хоть и не обязательно
            completableFuture.thenRunAsync(this::postRepaint, pool);

            return result;

        } catch (ExecutionException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    /**
     * Метод взаимодействия.
     * Формируем сообщение, в котором передаем результат выборки
     * @param result результат выборки (SELECT * FROM entries)
     */
    private void postResult(List<Entry> result) {
        Message message = Message.obtain(null, DATABASE_ENTRIES, result);
        mHandler.sendMessage(message);
    }

    /**
     * Метод взаимодействия.
     * Формируем сообщение с идентификатором запроса на отрисовку RecyclerView.
     * Мы говорим, что действия (insert, update, delete), которые были применены к базе данных, успешно
     * выполнены (потоки закончили свою работу), и можно перерисовывать RecyclerView (если необходимо)
     *
     * В настоящее время не используется, потому что эта логика делегирована Observer ContentProvider,
     * но сообщение я все равно отправляю, как дань прошлому варианту программы :)
     */
    private void postRepaint() {
        Message message = Message.obtain(null, REPAINT_REQUEST);
        mHandler.sendMessage(message);
    }

    /**
     * Метод установки Handler, который мы получаем из Activity. Фактически, нам нужен Looper, который
     * привязан к Handler в Activity, для того, чтобы сделать межпотоковое взаимодействие
     * @param handler Activity
     */
    public void setHandler(Handler handler) {
        mHandler = handler;
    }
}
