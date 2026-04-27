package com.whisperyao.dsplayer.adapters;

import android.os.Handler;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private int dragFlags;
    private final AbsAdapter mAdapter;
    private int swipeFlags;

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }

    public SimpleItemTouchHelperCallback(AbsAdapter adapter) {
        this.mAdapter = adapter;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (this.dragFlags != 0) {
            if (recyclerView.isComputingLayout()) {
                Handler handler = new Handler();
                final AbsAdapter absAdapter = this.mAdapter;
                Objects.requireNonNull(absAdapter);
                handler.post(() -> absAdapter.notifyDataSetChanged());
                return;
            }
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int i = this.mAdapter.isDragMode() ? 3 : 0;
        this.dragFlags = i;
        this.swipeFlags = 0;
        return makeMovementFlags(i, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        this.mAdapter.onMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }
}
