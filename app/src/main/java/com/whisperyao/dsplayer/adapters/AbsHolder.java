package com.whisperyao.dsplayer.adapters;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/* loaded from: classes.dex */
public abstract class AbsHolder<T> extends RecyclerView.ViewHolder {
    public abstract void showData(T t);

    public AbsHolder(View itemView) {
        super(itemView);
    }

    public int getItemViewPosition() {
        return getLayoutPosition();
    }

    public View getContentView() {
        return this.itemView;
    }
}