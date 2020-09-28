package linushdot.befundpostpcr;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class TestAdapter extends ListAdapter<Test, TestAdapter.ViewHolder> {

    public interface ItemListener {
        void onClick(Test test);
        boolean onLongClick(Test test);
    }

    private ItemListener itemListener;

    public void setItemListener(ItemListener listener) {
        this.itemListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        public ItemListener listener;
        public Test test;

        public TextView name;
        public int nameDefaultColor;
        public TextView result;
        public int resultDefaultColor;

        public ViewHolder(View view, ItemListener listener) {
            super(view);
            this.name = (TextView) view.findViewById(android.R.id.text1);
            this.nameDefaultColor = name.getCurrentTextColor();
            this.result = (TextView) view.findViewById(android.R.id.text2);
            this.resultDefaultColor = result.getCurrentTextColor();
            this.listener = listener;
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        public void bindTo(Test test) {
            this.test = test;
            name.setText(test.getName());
            if(test.getResultInfo() == null) {
                result.setText("");
            } else {
                result.setText(test.getResultInfo());
            }
            if(test.toBeDeleted) {
                name.setTextColor(Color.RED);
                result.setTextColor(Color.RED);
            } else {
                name.setTextColor(nameDefaultColor);
                result.setTextColor(resultDefaultColor);
            }
        }

        @Override
        public void onClick(View v) {
            if(listener != null) {
                listener.onClick(test);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if(listener != null) {
                return listener.onLongClick(test);
            }
            return false;
        }
    }

    public TestAdapter() {
        super(new DiffUtil.ItemCallback<Test>() {
            @Override
            public boolean areItemsTheSame(@NonNull Test oldItem, @NonNull Test newItem) {
                return oldItem.id.equals(newItem.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Test oldItem, @NonNull Test newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.test_item, parent, false);
        return new ViewHolder(view, itemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }
}
