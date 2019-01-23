package home.stanislavpoliakov.meet11_practice;

import android.os.Bundle;

/**
 * Интерфейс взаимодействия фрагментов и Activity по схеме Fragment -> Activity
 */
public interface CRUDable {
    void create(Entry entry);
    void update(Bundle entryInfo);
    void delete(Entry entry);
}
