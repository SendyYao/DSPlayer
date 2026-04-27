package com.whisperyao.dsplayer.adapters;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.widget.DpPxConverter;
import eu.davidea.fastscroller.FastScroller;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public abstract class AbsAdapter<T> extends RecyclerView.Adapter<AbsHolder> implements View.OnClickListener {
    private boolean bIsCheckMode;
    private boolean bIsDragMode;
    protected boolean bIsLeft;
    private boolean bIsListMode;
    protected Callback callback;
    protected boolean[] mCheckedList;
    private RecyclerDecoration mDecoration;
    protected RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    protected int selPos;
    protected OnItemClickListener<T> mOnItemClickListener = null;
    protected ArrayList<T> data = new ArrayList<>();
    protected ArrayList<T> orig = new ArrayList<>();
    protected ItemTouchHelper touchHelper = null;
    private View mHeader = null;
    private View mEmptyView = null;
    private boolean bShowHeader = false;
    protected final int HEADER = -1;
    protected final int LIST = 1;
    protected final int GRID = 2;
    protected final int list_press = R.color.list_press_over_light;
    protected final int transparent = R.color.transparent;
    protected FastScroller.Delegate mFastScrollerDelegate = new FastScroller.Delegate();

    public interface Callback {
        void onItemSelected(int count);

        void onTrackOrderChanged(int playingPos);
    }

    public interface OnItemClickListener<T> {
        void onItemClick(View view, T item, int position);
    }

    public abstract void onBind(AbsHolder holder, T t, int position);

    public void onBindHeaderFooter(AbsHolder holder) {
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public abstract AbsHolder onCreateViewHolder(ViewGroup parent, int viewType);

    public AbsAdapter() {
    }

    public AbsAdapter(Callback callback) {
        this.callback = callback;
    }

    public void setIsListMode(boolean b) {
        this.bIsListMode = b;
        checkDividers();
        notifyDataSetChanged();
    }

    public boolean isListMode() {
        return this.bIsListMode;
    }

    public void checkDividers() {
        float fDp2px;
        if (this.mDecoration == null && this.mRecyclerView != null) {
            this.mDecoration = new RecyclerDecoration(this.mRecyclerView.getContext());
            // StateManager.getInstance().isMobile()
            if (true) {
                fDp2px = DpPxConverter.dp2px(4.0f, this.mRecyclerView.getContext());
            } else {
                fDp2px = DpPxConverter.dp2px(8.0f, this.mRecyclerView.getContext());
            }
            this.mDecoration.setSpacing((int) fDp2px);
        }
        RecyclerDecoration recyclerDecoration = this.mDecoration;
        if (recyclerDecoration != null) {
            this.mRecyclerView.removeItemDecoration(recyclerDecoration);
            this.mDecoration.setStartDrawingPosition(getHeaderOffset());
            this.mDecoration.drawDivider(this.bIsListMode);
            this.mDecoration.showOffset(!this.bIsListMode);
            this.mRecyclerView.addItemDecoration(this.mDecoration);
        }
    }

    protected int getHeaderOffset() {
        return this.bShowHeader ? 1 : 0;
    }

    public void setIsCheckMode(boolean b) {
        this.bIsCheckMode = b;
        if (b) {
            this.mCheckedList = new boolean[getRealItemCount()];
        }
        notifyDataSetChanged();
    }

    public boolean isCheckMode() {
        return this.bIsCheckMode;
    }

    public void checkItem(int position) {
        int realDataPosition = getRealDataPosition(position);
        if (realDataPosition < 0 || realDataPosition >= getItemCount()) {
            return;
        }
        // !r0[realDataPosition]
        this.mCheckedList[realDataPosition] = !mCheckedList[realDataPosition];
        checkSum();
    }

    public boolean isItemChecked(int position) {
        int realDataPosition = getRealDataPosition(position);
        return realDataPosition >= 0 && realDataPosition < getRealItemCount() && this.mCheckedList[realDataPosition];
    }

    public void checkAll() {
        modifyAll(true);
    }

    public void unCheckAll() {
        modifyAll(false);
    }

    private void modifyAll(boolean b) {
        int i = 0;
        while (true) {
            boolean[] zArr = this.mCheckedList;
            if (i < zArr.length) {
                zArr[i] = b;
                i++;
            } else {
                checkSum();
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void setHeader(View view) {
        this.bShowHeader = true;
        this.mHeader = view;
        checkDividers();
        notifyDataSetChanged();
    }

    public final View getHeader() {
        return this.mHeader;
    }

    public void disableHeader() {
        this.bShowHeader = false;
        checkDividers();
        notifyDataSetChanged();
        this.mHeader = null;
    }

    public boolean isShowHeader() {
        return this.bShowHeader;
    }

    private void checkSum() {
        Callback callback = this.callback;
        if (callback != null) {
            callback.onItemSelected(getCheckedItemCount());
        }
    }

    protected final int getCheckedItemCount() {
        int i = 0;
        for (boolean z : this.mCheckedList) {
            if (z) {
                i++;
            }
        }
        return i;
    }

    public void setIsDragMode(boolean b) {
        this.bIsDragMode = b;
        if (b) {
            this.orig.clear();
            this.orig.addAll(this.data);
        }
        notifyDataSetChanged();
    }

    public boolean isDragMode() {
        return this.bIsDragMode;
    }

    public void applyOrder() {
        this.orig.clear();
    }

    public void undoOrder() {
        this.data.clear();
        this.data.addAll(this.orig);
        notifyDataSetChanged();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int position) {
        if (this.bShowHeader && position == 0) {
            return -1;
        }
        return this.bIsListMode ? 1 : 2;
    }

    public final void setSpan(int span) {
        RecyclerView.LayoutManager layoutManager = this.mLayoutManager;
        if (layoutManager instanceof GridLayoutManager) {
            ((GridLayoutManager) layoutManager).setSpanCount(span);
        }
        RecyclerDecoration recyclerDecoration = this.mDecoration;
        if (recyclerDecoration != null) {
            recyclerDecoration.setSpan(span);
            checkDividers();
        }
    }

    public int getSpan() {
        RecyclerView.LayoutManager layoutManager = this.mLayoutManager;
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        }
        return 1;
    }

    public void addEmptyView(final View view, final boolean reloadImmediately) {
        this.mEmptyView = view;
        if (reloadImmediately) {
            notifyDataSetChanged();
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        this.mLayoutManager = layoutManager;
        if (layoutManager instanceof GridLayoutManager) {
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(getSpanSizeLookup());
        }
        checkDividers();
        FastScroller.Delegate delegate = this.mFastScrollerDelegate;
        if (delegate != null) {
            delegate.onAttachedToRecyclerView(recyclerView);
        }
    }

    public void setFastScroller(FastScroller fastScroller) {
        this.mFastScrollerDelegate.setFastScroller(fastScroller);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        FastScroller.Delegate delegate = this.mFastScrollerDelegate;
        if (delegate != null) {
            delegate.onDetachedFromRecyclerView(recyclerView);
        }
        this.mRecyclerView = null;
    }

    protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() { // from class: com.synology.dsaudio.adapters.AbsAdapter.1
            @Override // androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
            public int getSpanSize(int position) {
                if (AbsAdapter.this.bShowHeader && position == 0) {
                    return ((GridLayoutManager) AbsAdapter.this.mLayoutManager).getSpanCount();
                }
                if (AbsAdapter.this.bIsListMode) {
                    return ((GridLayoutManager) AbsAdapter.this.mLayoutManager).getSpanCount();
                }
                return 1;
            }
        };
    }

    public void setData(List<T> data) {
        this.data.clear();
        if (data != null) {
            this.data.addAll(data);
        }
        RecyclerDecoration recyclerDecoration = this.mDecoration;
        if (recyclerDecoration != null) {
            recyclerDecoration.setStartDrawingPosition(getHeaderOffset());
        }
        View view = this.mEmptyView;
        if (view != null) {
            view.setVisibility(getItemCount() == 0 ? 0 : 8);
        }
        notifyDataSetChanged();
    }

    public ArrayList<T> getData() {
        return this.data;
    }

    public final void setIsLeft(boolean b) {
        this.bIsLeft = b;
    }

    public final boolean isLeft() {
        return this.bIsLeft;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return getRealItemCount() + (this.bShowHeader ? 1 : 0);
    }

    public final int getRealItemCount() {
        ArrayList<T> arrayList = this.data;
        if (arrayList == null) {
            return 0;
        }
        return arrayList.size();
    }

    protected int getRealDataPosition(int i) {
        return this.bShowHeader ? i - 1 : i;
    }

    @Override
    public void onBindViewHolder(AbsHolder holder, int position) {
        holder.itemView.setTag(position);
        int realDataPosition = getRealDataPosition(position);
        if (realDataPosition < 0 || realDataPosition == getRealItemCount()) {
            onBindHeaderFooter(holder);
        } else {
            onBind(holder, this.data.get(realDataPosition), realDataPosition);
        }
    }

    public int getScrollToPosition() {
        RecyclerView.LayoutManager layoutManager = this.mLayoutManager;
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        }
        return 0;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        int realDataPosition = getRealDataPosition(((Integer) view.getTag()).intValue());
        int iIntValue = ((Integer) view.getTag()).intValue();
        if (this.mOnItemClickListener != null) {
            if (isCheckMode()) {
                checkItem(iIntValue);
                notifyItemChanged(iIntValue);
            } else {
                this.selPos = iIntValue;
                this.mOnItemClickListener.onItemClick(view, this.data.get(realDataPosition), realDataPosition);
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.mOnItemClickListener = listener;
    }

    public void setTouchHelper(ItemTouchHelper h) {
        this.touchHelper = h;
    }

    public void onMove(int fromPosition, int toPosition) {
        Collections.swap(this.data, fromPosition, toPosition);
        swapCheckList(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    private void swapCheckList(int i, int j) {
        boolean[] zArr = this.mCheckedList;
        if (zArr == null) {
            return;
        }
        boolean z = zArr[i];
        zArr[i] = zArr[j];
        zArr[j] = z;
    }
}
