package home.stanislavpoliakov.meet11_practice;

import android.support.v7.util.DiffUtil;

import java.util.List;

// Сюда и добавить-то нечего! DiffUtil.Callback какой есть :)

public class DiffCall extends DiffUtil.Callback {
    private List<Entry> oldData, newData;

    public DiffCall(List<Entry> oldData, List<Entry> newData) {
        this.oldData = oldData;
        this.newData = newData;
    }

    @Override
    public int getOldListSize() {
        return oldData.size();
    }

    @Override
    public int getNewListSize() {
        return newData.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return false;
    }
}
