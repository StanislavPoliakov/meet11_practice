package home.stanislavpoliakov.meet11_practice;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.Inflater;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "meet11_logs";
    private List<Entry> data;

    /**
     * Конструктор
     * @param newData слепок базы, который необходимо отрисовать. Мы будем считать, что данные,
     *                полученные в конструкторе, то есть при создании, - это oldData, с точки зрения
     *                DiffUtil
     */
    public MyAdapter(List<Entry> newData) {
        this.data = new ArrayList<>();
        this.data.addAll(newData);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder, parent, false);
        return new MyViewHolder(view);
    }

    /**
     * Метод запуска DiffUtil для подсчета различий и необходимости отрисовки RecyclerView
     * @param newData новые данные (слепок с внесенными, но пока не отрисованными изменениями)
     *                глобальная переменная data хранит "старые" данные (oldData)
     */
    public void onNewData(List<Entry> newData) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffCall(data, newData));
        result.dispatchUpdatesTo(this);

        // Сохраняем текущий слепок
        data.clear();
        data.addAll(newData);

        notifyDataSetChanged(); // Чтобы RecyclerView не мигал :) "Чтобы цепь не слетала" (с)
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.title.setText(data.get(position).getTitle());
        holder.body.setText(data.get(position).getText());
        holder.timestamp.setText(String.valueOf(data.get(position).getId()));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title, body, timestamp;

        public MyViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            body = itemView.findViewById(R.id.body);
            timestamp = itemView.findViewById(R.id.timestamp);

            // Добавляем контекстное меню по longTouch
            itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                menu.add(0, getAdapterPosition(), 0, "Edit");
                menu.add(0, getAdapterPosition(), 0, "Delete");
            });
        }
    }
}
