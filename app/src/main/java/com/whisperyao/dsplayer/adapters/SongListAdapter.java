package com.whisperyao.dsplayer.adapters;


import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.databinding.SongItemFrescoBinding;
import com.whisperyao.dsplayer.databinding.TabletSongItemFrescoBinding;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;
import java.util.ArrayList;

public class SongListAdapter extends AbsAdapter<SongItem> {
    private final int VIEW_TYPE_FILE_NARROW;
    private final int VIEW_TYPE_FILE_WIDE;
    private final int VIEW_TYPE_FOLDER;
    private ArrayList<Integer> mHoldSongIndex;
    private Common.ContainerType mType;

    public SongListAdapter(AbsAdapter.Callback c) {
        super(c);
        this.VIEW_TYPE_FOLDER = 0;
        this.VIEW_TYPE_FILE_NARROW = 1;
        this.VIEW_TYPE_FILE_WIDE = 2;
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (-1 == viewType) {
            return new HeaderHolder(getHeader());
        }
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(parent.getContext());
        if (StateManager.getInstance().isMobileLayout()) {
            SongItemFrescoBinding songItemFrescoBindingInflate = SongItemFrescoBinding.inflate(layoutInflaterFrom, parent, false);
            songItemFrescoBindingInflate.getRoot().setOnClickListener(this);
            return new ListHolder(songItemFrescoBindingInflate);
        }
        TabletSongItemFrescoBinding tabletSongItemFrescoBindingInflate = TabletSongItemFrescoBinding.inflate(layoutInflaterFrom, parent, false);
        tabletSongItemFrescoBindingInflate.getRoot().setOnClickListener(this);
        return new ListHolder(tabletSongItemFrescoBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, SongItem song, int position) {
        if (holder instanceof ListHolder) {
            holder.showData(this.data.get(position));
        }
    }

    @Override
    public void onBindHeaderFooter(AbsHolder holder) {
        boolean z = holder instanceof HeaderHolder;
    }

    public void setType(Common.ContainerType type) {
        this.mType = type;
    }

    public ArrayList<SongItem> getSelectedItems() {
        ArrayList<SongItem> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mCheckedList.length; i++) {
            if (this.mCheckedList[i]) {
                arrayList.add(this.data.get(i));
            }
        }
        return arrayList;
    }

    @Override
    public void onMove(int fromPosition, int toPosition) {
        super.onMove(fromPosition, toPosition);
        if (this.callback != null) {
            int iMin = Math.min(fromPosition, toPosition);
            int iMax = Math.max(fromPosition, toPosition);
            int i = iMin;
            while (true) {
                if (i > iMax) {
                    break;
                }
                if (((SongItem) this.data.get(i)).getIconStatus() == Item.IconStatus.PLAYING) {
                    this.callback.onTrackOrderChanged(i);
                    SynoLog.e("pos", "from " + iMin + " to " + iMax + ", found " + i);
                    break;
                }
                i++;
            }
        }
        if (this.mHoldSongIndex == null) {
            this.mHoldSongIndex = new ArrayList<>();
            for (int i2 = 0; i2 < this.data.size(); i2++) {
                this.mHoldSongIndex.add(i2, Integer.valueOf(i2));
            }
        }
        this.mHoldSongIndex.add(toPosition, this.mHoldSongIndex.remove(fromPosition));
    }

    @Override
    public void applyOrder() {
        this.mHoldSongIndex = null;
        super.applyOrder();
    }

    @Override
    public void undoOrder() {
        this.mHoldSongIndex = null;
        super.undoOrder();
    }

    class ListHolder extends AbsHolder<SongItem> {
        private TextView album;
        private TextView artist;
        private final SimpleDraweeView cover;
        private final ImageView drag;
        private final TextView duration;
        private final ImageView icon;
        private TextView itemTime;
        private final CheckBox mark;
        private final ImageView shortcut;
        private TextView subtitle;
        private final TextView title;

        private void initView() {
            this.drag.setOnTouchListener((view, motionEvent) -> {
                if (SongListAdapter.this.touchHelper == null) {
                    return true;
                }
                SongListAdapter.this.touchHelper.startDrag(this);
                return true;
            });
            if (StateManager.getInstance().isMobileLayout() || !SongListAdapter.this.mType.equals(Common.ContainerType.RANDOM100_MODE)) {
                this.cover.setVisibility(View.GONE);
            }
            if (StateManager.getInstance().isMobileLayout()) {
                this.duration.setVisibility(View.GONE);
            }
        }

        ListHolder(SongItemFrescoBinding binding) {
            super(binding.getRoot());
            this.drag = binding.dragHandle;
            this.mark = binding.SongItemCheckBox;
            this.cover = binding.SongItemCover;
            this.icon = binding.SongItemIcon;
            this.title = binding.SongItemTitle;
            this.subtitle = binding.SongItemSubTitle;
            this.itemTime = binding.SongItemTime;
            this.shortcut = binding.SongItemShortCut;
            this.duration = binding.SongItemTime;
            initView();
        }

        ListHolder(TabletSongItemFrescoBinding binding) {
            super(binding.getRoot());
            this.drag = binding.dragHandle;
            this.mark = binding.SongItemCheckBox;
            this.cover = binding.SongItemCover;
            this.icon = binding.SongItemIcon;
            this.title = binding.SongItemTitle;
            this.itemTime = binding.SongItemTime;
            this.shortcut = binding.SongItemShortCut;
            this.duration = binding.SongItemTime;
            this.album = binding.SongItemAlbum;
            this.artist = binding.SongItemArtist;
            initView();
        }

        @Override
        public void showData(SongItem item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(SongListAdapter.this);
            this.mark.setTag(getAdapterPosition());
            this.mark.setOnClickListener(SongListAdapter.this);
            this.title.setText(item.getTitle());
            TextView textView = this.subtitle;
            if (textView != null) {
                textView.setText(item.getSongDescription());
            }
            TextView textView2 = this.artist;
            if (textView2 != null) {
                textView2.setText(item.getArtist());
            }
            TextView textView3 = this.album;
            if (textView3 != null) {
                textView3.setText(item.getAlbum());
            }
            if (this.cover.getVisibility() == View.VISIBLE) {
                new CoverUriLoader().with(this.cover).placeHolder(SongListAdapter.this.mType).load(item);
            }
            TextView textView4 = this.duration;
            if (textView4 != null) {
                textView4.setText(item.getTimeString());
            }
            if (SongListAdapter.this.isCheckMode()) {
                this.shortcut.setVisibility(View.GONE);
                this.mark.setVisibility(View.VISIBLE);
                this.mark.setChecked(SongListAdapter.this.isItemChecked(getAdapterPosition()));
            } else {
                this.mark.setVisibility(View.GONE);
                this.shortcut.setVisibility(View.VISIBLE);
            }
            if (SongListAdapter.this.mType.isShowRatingIcon()) {
                TextView textView5 = this.duration;
                if (textView5 != null) {
                    textView5.setVisibility(View.GONE);
                }
                int rating = (int) item.getRating();
                this.icon.setImageResource(
                        rating >= 5 ? R.drawable.icon_rating5 :
                                rating == 4 ? R.drawable.icon_rating4 :
                                        rating == 3 ? R.drawable.icon_rating3 :
                                                rating == 2 ? R.drawable.icon_rating2 :
                                                        rating == 1 ? R.drawable.icon_rating1 :
                                                                R.drawable.icon_rating0);
            }
        }
    }

    class HeaderHolder extends AbsHolder {
        @Override
        public void showData(Object o) { }

        HeaderHolder(View itemView) {
            super(itemView);
        }
    }
}
