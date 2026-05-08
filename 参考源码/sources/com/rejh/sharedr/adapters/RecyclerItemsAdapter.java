package com.rejh.sharedr.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import bin.p002mt.plus.TranslationData.R;
import com.rejh.sharedr.ActShareReplace;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class RecyclerItemsAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static String APPTAG = "Sharedr";
    private static AdapterCallback mAdapterCallback;
    private ArrayList<ActShareReplace.Item> mDataSet;
    private boolean mUseGrid;

    public interface AdapterCallback {
        void onRecycleViewItemClick(View view, int i);

        boolean onRecycleViewItemLongClick(View view, int i);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView label;
        private final View view;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.label = (TextView) view.findViewById(R.id.label);
            this.icon = (ImageView) view.findViewById(R.id.icon);
        }

        public View getView() {
            return this.view;
        }

        public TextView getLabelView() {
            return this.label;
        }

        public ImageView getIconView() {
            return this.icon;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public RecyclerItemsAdapter(Context context, ArrayList<ActShareReplace.Item> arrayList, boolean z) {
        mAdapterCallback = (AdapterCallback) context;
        this.mDataSet = arrayList;
        this.mUseGrid = z;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(this.mUseGrid ? R.layout.gridview_item_activity : R.layout.listview_item_activity, viewGroup, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        ActShareReplace.Item item = this.mDataSet.get(i);
        viewHolder.getLabelView().setText(item.labelOverride != null ? item.labelOverride : item.label);
        viewHolder.getIconView().setImageDrawable(item.icon);
        viewHolder.getView().setOnClickListener(new View.OnClickListener() { // from class: com.rejh.sharedr.adapters.RecyclerItemsAdapter.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                RecyclerItemsAdapter.mAdapterCallback.onRecycleViewItemClick(view, i);
            }
        });
        viewHolder.getView().setOnLongClickListener(new View.OnLongClickListener() { // from class: com.rejh.sharedr.adapters.RecyclerItemsAdapter.2
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                return RecyclerItemsAdapter.mAdapterCallback.onRecycleViewItemLongClick(view, i);
            }
        });
    }

    public void update() {
        notifyDataSetChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mDataSet.size();
    }
}
