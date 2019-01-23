package home.stanislavpoliakov.meet11_practice;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CRUDable{
    private static final String TAG = "meet11_logs";

    // Элементы адреса Shared-таблицы в ContentProvider, где:
    // AUTHORITY = the symbolic name of the entire provider (its authority)
    // ENTRIES_TABLE = a name that points to a table (a path). Room database name, в нашем случае
    private static final String AUTHORITY = "content_provider";
    private static final String ENTRIES_TABLE = "new_database";

    private UIHandler uiHandler = new UIHandler();

    // Слепок базы данных
    private volatile List<Entry> data;

    private MyAdapter mAdapter;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private ContentObserver mContentObserver;
    private boolean isFirstLaunch = true;

    /**
     * Класс Observer для ContentProvider.
     * Создается в onCreate, регистрируется в onResume и снимаем регистрацию в onPause, что
     * делает этот Observer актуальным только для изменений внутри самого приложения (не для
     * регистрации внешних изменений, поскольку две Activity одновременно мы не видим).
     *
     * В принципе, именно это заставило меня ставить проверку состояния базы в onCreate, хотя
     * сейчас мне кажется более изящным отправлять BroadCast
     */
    private class MyObserver extends ContentObserver {

        public MyObserver(Handler handler) {
            super(handler);
        }

        /**
         * Метод, в котором мы реагируем на изменения в ContentProvider. Просто перерисовываем
         * RecyclerView
         * @param selfChange ?
         */
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            repaintRecycler();
            //Log.d(TAG, "onChange: ");
        }
    }

    /**
     * Класс Handler'-а, который обрабатывает взаимодействие UI-потока с потоками для базы данных
     */
    private class UIHandler extends Handler {
        @Override
        public void handleMessage (Message msg){
            // Получаем сообщение с записями базы данных (SELECT * FROM entries) в теле сообщения.
            // Если это первый запуск системы - продолжаем инициализацию, если нет - перерисовываем
            // изменения в RecyclerView
            if (msg.what == DatabaseManager.DATABASE_ENTRIES) {
                data = (List<Entry>) msg.obj;
                if (isFirstLaunch) initRecyclerView();
                else repaintRecycler();

            // Это наследие предыдущих состояний системы. По факту, потоки, работающие с базой
            // данных до сих пор присылают сообщение-запрос для отрисовки результатов в RecyclerView,
            // но реализация этого поведения перенесена в Observer
            } else if (msg.what == DatabaseManager.REPAINT_REQUEST) {
                //repaintRecycler();
            }
        }
    };

    /**
     * @param savedInstanceState сохраненное состояние
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Инициализируем Observer для ContentProvider
        mContentObserver = new MyObserver(new Handler(Looper.getMainLooper()));

        init();
    }

    /**
     * При восстановлении рабочего состояния Activity регистрируем Observer на изменения в
     * ContentProvider, а также проверяем, изменилось ли текущее состояние базы данных, и, если
     * изменилось, инициируем отрисовку RecyclerView
     * isFirstLaunch - при первом запуске мы не перерисовываем RecyclerView, т.к. он не инициализирован
     */
    @Override
    protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/#"),
                true, mContentObserver);
       if (!isFirstLaunch) checkForUpdates();
    }

    /**
     * Метод обновления RecyclerView после возобновления рабочего сосотяния (onCreate)
     * Я не инициирую отрисовку результатов здесь, потому что результат отрисовывается
     * после чтения всех записей в Handler. Это наследие от предыдущих версий программы,
     * когда не надо было реализовывать ContentProvider
     */
    private void checkForUpdates() {
        // Получаем адрес таблицы с записями
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE);

        // Получаем записи всей таблицы в виде объекта Cursor (SELECT * FROM entries")
        getContentResolver().query(CONTENT_URI, null, null, null, null);
    }

    /**
     * При сворачивании Activity (потере фокуса) снимаем регистрацию Observer для Content
     * Provider
     */
    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mContentObserver);
    }

    /**
     * Метод основной инициализации. Получаем объект DatabaseManager (Singleton) и передаем
     * ему Handler для общения (если я правильно понимаю, то, фактически, передаем ему Looper).
     * Далее считываем состояние базы, дальнейшая инициализация переходит в Handler (при чтении
     * состояния базы). Здесь же инициализируем Floating Action Button
     */
    private void init() {
        // Открываем новый Dialog-Fragment при нажатии на FAB
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(new CreateFragment(), "create dialog")
                    .commitNow();
        });

        DatabaseManager dbManager = DatabaseManager.getInstance(this);
        dbManager.setHandler(uiHandler);

        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE);
        getContentResolver().query(CONTENT_URI, null, null, null, null);
    }

    /**
     * Метод инициализации  RecyclerView, продолжающий общую инициализацию. Запускается через Handler
     * при первом запуске (isFirstLaunch)
     */
    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mAdapter = new MyAdapter(data);
        recyclerView.setAdapter(mAdapter);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);

        // Обнуляем флаг. Думаю, что корректнее переименовать флаг в isRecyclerInitiated
        isFirstLaunch = false;
    }

    /**
     * Метод инициализации dialog-фрагмента для редактирования записи. Запуск по выбору соотвествующего
     * пункта ("Edit") в контекстном меню.
     * @param itemPosition позиция элемента, который мы хотим редактировать
     */
    private void initEditDialog(int itemPosition) {
        // Получаем запись из списка по номеру позиции
        Entry entry = data.get(itemPosition);

        // Поскольку нам необходимо заполнить поля окна редактирования сущестующими значениями, а
        // сам объект в Bundle мы передать не можем, парсим запись на допустимые типы (String) и
        // передаем состояние записи фрагменту в качестве аргументов
        Bundle bundle = new Bundle();
        bundle.putString("title", entry.getTitle());
        bundle.putString("body", entry.getText());

        // Отдельно стоит сказать про позицию элемента. В дальнейшем, после успешного редактирования,
        // мы запускаем метод обновления записи. Но по причине того, что в Bundle не помещается Object,
        // мы должны заново найти запись в списке по порядковому номеру, чтобы вытащить из нее id.
        // В принципе, можно предавть сразу id, но тогда для изменения состояния записи в списке
        // придется искать запись по id, что не очень удобно
        bundle.putInt("item position", itemPosition);


        // Запускаем dialog-fragment
        EditFragment fragment = new EditFragment();
        fragment.setArguments(bundle);

        fragmentManager.beginTransaction()
                .add(fragment, "edit dialog")
                .commitNow();
    }

    /**
     * Метод обработки выбора элементов контекстного меню. Меню привязано к ViewHolder
     * @param item элемент меню. В нашем случае "Edit" или "Delete"
     * @return выбрано значение или нет
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if ("Edit".equals(item.getTitle())) initEditDialog(item.getItemId());
        else if ("Delete".equals(item.getTitle())) {
            Entry entry = data.get(item.getItemId());
            delete(entry);
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Метод, отвечающий за отрисовку изменений в RecyclerView. Изменения обсчитываются через
     * DiffUtils, Adapter хранит "отрисованную" версию списка, мы передаем ему новую версию, котороую
     * необходимо оценить и перерисовать, если требуется
     */
    private void repaintRecycler() {
        if (data != null || !data.isEmpty()) mAdapter.onNewData(data);
    }

    /**
     * Метод создания новой записи в слепке и в базе данных через ContentProvider. Это метод интерфейса
     * CRUDable для взаимодействия фрагмента с Activity. Согласен, название метода create - некорректно в рамках
     * Android-разработки. "Insert" выглядит более корректным
     * @param entry запись, которую необходимо добавить
     */
    @Override
    public void create(Entry entry) {
        // Нам нужно любое id, чтобы добавить запись. Конечное значение id будет установлено в момент
        // добавления записи в базу данных (id = primary key, autoincrement). Мы получим актуальное
        // значение в конце итерации
        int id = 0;

        // Формируем адрес нового элемента (с нашим "каким-нибудь" значением id)
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + id);

        // Добавляем элемент (реализация в ContentProvider) и получаем актуальный URI нового элемента,
        // который, среди прочего, содержит актуальный id (добавила база)
        CONTENT_URI = getContentResolver().insert(CONTENT_URI, ConvertUtils.convertEntryToValues(entry));

        // Получаем значение id в виде String и преобразуем к Integer
        String stringID = CONTENT_URI.getLastPathSegment();
        id = Integer.parseInt(stringID);

        // Обновляем значение id в записи и добаляем запись к текущему слепку базы данных
        entry.setId(id);
        data.add(entry);
    }

    /**
     * Метод обновления записи в слепке и в базе данных через ContentProvider. Это метод интерфейса
     * CRUDable для взаимодействия фрагмента с Activity.
     * @param entryInfo информация, фактически, содержащая элементы записи, поскольку саму запись в
     *                  Bundle мы передать не можем. Также содержит информацию о порядковом номере
     *                  записи в списке, для поиска
     */
    @Override
    public void update(Bundle entryInfo) {
        // Ищем запись в списке по порядковому номеру (полученному в Bundle)
        Entry entry = data.get(entryInfo.getInt("item position"));

        // Устанавливаем актуальные значения
        entry.setTitle(entryInfo.getString("title"));
        entry.setText(entryInfo.getString("body"));

        // Получаем id для адреса ContentProvider и производим изменения в существующей записи
        // базы данных (реализация в ContentProvider).
        int id = entry.getId();
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + id);

        // Поскольку напрямую вставить запись в базу (точнее, в контент) мы не можем из-за реализации
        // метода update в ContentProvider (запись должна быть в ContentValues), конвертируем наш
        // объект Entry в подходящий формат ContentValues. Реализация в классе ConvertUtils
        getContentResolver().update(CONTENT_URI, ConvertUtils.convertEntryToValues(entry), null, null);
    }

    /**
     * Метод удаления записи в слепке и в базе данных через ContentProvider. Это метод интерфейса
     * CRUDable, не реализован во взаимодействии с фрагментами, но здесь просто для порядка :)
     *
     * Фактическое удаление записи происходит по id, но метод имеет "старую" сигнатуру, как наследие
     * от предыдущих версий программы
     *
     * @param entry запись, которую необходимо удалить
     */
    @Override
    public void delete(Entry entry) {
        // Получаем id записи и формируем адрес записи в ContentProvider
        int id = entry.getId();
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + id);

        // Удаляем элемент базы данных. Реализация в ContentProvider
        getContentResolver().delete(CONTENT_URI, null, null);

        // Удаляем элемент в текущем слепке
        data.remove(entry);
    }

    /**
     * Методы создания меню в toolbar. Там должны "жить" SharedPreferences. В текущей версии
     * программы не реализовывал
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
