package com.whisperyao.dsplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.databinding.SongItemFrescoBinding;
import com.whisperyao.dsplayer.databinding.TabletSongItemFrescoBinding;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;
import java.util.ArrayList;

public class PlaylistSongListAdapter extends AbsAdapter<SongItem> {
    private ArrayList<Integer> mHoldSongIndex;

    public PlaylistSongListAdapter(AbsAdapter.Callback c) {
        super(c);
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void setIsDragMode(boolean b) {
        if (b) {
            this.mHoldSongIndex = new ArrayList<>();
            for (int i = View.VISIBLE; i < this.data.size(); i++) {
                this.mHoldSongIndex.add(i, i);
            }
        }
        super.setIsDragMode(b);
    }

    public ArrayList<SongItem> getSelectedItems() {
        ArrayList<SongItem> arrayList = new ArrayList<>();
        for (int i = View.VISIBLE; i < this.mCheckedList.length; i++) {
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
                if (this.data.get(i).getIconStatus() == Item.IconStatus.PLAYING) {
                    this.callback.onTrackOrderChanged(i);
                    SynoLog.e("pos", "from " + iMin + " to " + iMax + ", found " + i);
                    break;
                }
                i++;
            }
        }
        this.mHoldSongIndex.add(toPosition, this.mHoldSongIndex.remove(fromPosition));
    }

    public ArrayList<Integer> getHoldSongIndex() {
        ArrayList<Integer> arrayList = this.mHoldSongIndex;
        return arrayList == null ? new ArrayList<>() : arrayList;
    }

    public ArrayList<Integer> getCheckedSongIndex() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        if (this.mCheckedList != null) {
            for (int i = View.VISIBLE; i < this.mCheckedList.length; i++) {
                if (this.mCheckedList[i]) {
                    arrayList.add(i);
                }
            }
        }
        return arrayList;
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
        private final CheckBox mark;
        private final ImageView shortcut;
        private TextView subtitle;
        private final TextView title;

        private void initView() {
            drag.setOnTouchListener((view, event) -> {
                if (PlaylistSongListAdapter.this.touchHelper == null) {
                    return true;
                }

                PlaylistSongListAdapter.this
                        .touchHelper
                        .startDrag(this);

                return true;
            });
        }


        ListHolder(SongItemFrescoBinding binding) {
            super(binding.getRoot());
            this.title = binding.SongItemTitle;
            this.subtitle = binding.SongItemSubTitle;
            this.duration = binding.SongItemTime;
            this.cover = binding.SongItemCover;
            this.icon = binding.SongItemIcon;
            this.mark = binding.SongItemCheckBox;
            this.shortcut = binding.SongItemShortCut;
            this.drag = binding.dragHandle;
            initView();
        }

        ListHolder(TabletSongItemFrescoBinding binding) {
            super(binding.getRoot());
            this.title = binding.SongItemTitle;
            this.duration = binding.SongItemTime;
            this.cover = binding.SongItemCover;
            this.album = binding.SongItemAlbum;
            this.artist = binding.SongItemArtist;
            this.icon = binding.SongItemIcon;
            this.mark = binding.SongItemCheckBox;
            this.shortcut = binding.SongItemShortCut;
            this.drag = binding.dragHandle;
            initView();
        }

        @Override
        public void showData(SongItem item) {
            this.title.setText(item.getTitle());
            this.duration.setVisibility(View.VISIBLE);
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(PlaylistSongListAdapter.this);
            this.mark.setTag(getAdapterPosition());
            this.mark.setOnClickListener(PlaylistSongListAdapter.this);
            if (PlaylistSongListAdapter.this.isCheckMode()) {
                PlaylistSongListAdapter playlistSongListAdapter = PlaylistSongListAdapter.this;
                this.mark.setChecked(playlistSongListAdapter.isItemChecked(playlistSongListAdapter.getRealDataPosition(getAdapterPosition())));
            }
            if (!StateManager.getInstance().isMobileLayout()) {
                this.cover.setVisibility(View.VISIBLE);
                if (item.isRadio()) {
                    this.duration.setText("--:--");
                    this.cover.setImageResource(R.drawable.thumbnail_radio);
                } else {
                    this.duration.setText(item.getTimeString());
                    new CoverUriLoader().with(this.cover).placeHolder(R.drawable.thumbnail_album).load(item);
                }
            } else {
                this.cover.setVisibility(View.GONE);
            }
            if (this.subtitle != null) {
                if (item.isRadio()) {
                    this.subtitle.setText(R.string.str_internet_radio);
                } else {
                    this.subtitle.setText(item.getSongDescription());
                }
            }
            TextView textView = this.album;
            if (textView != null) {
                textView.setVisibility(View.VISIBLE);
                this.album.setText(item.getAlbum());
            }
            TextView textView2 = this.artist;
            if (textView2 != null) {
                textView2.setVisibility(View.VISIBLE);
                this.artist.setText(item.getArtist());
            }
            if (PlaylistSongListAdapter.this.isDragMode()) {
                this.mark.setVisibility(View.GONE);
                this.shortcut.setVisibility(View.GONE);
                this.drag.setVisibility(View.VISIBLE);
                this.duration.setVisibility(View.GONE);
                this.cover.setVisibility(View.GONE);
                TextView textView3 = this.album;
                if (textView3 != null) {
                    textView3.setVisibility(View.GONE);
                }
                TextView textView4 = this.artist;
                if (textView4 != null) {
                    textView4.setVisibility(View.GONE);
                    return;
                }
                return;
            }
            if (PlaylistSongListAdapter.this.isCheckMode()) {
                this.shortcut.setVisibility(View.GONE);
                this.drag.setVisibility(View.GONE);
                this.mark.setVisibility(View.VISIBLE);
            } else {
                this.mark.setVisibility(View.GONE);
                this.drag.setVisibility(View.GONE);
                this.duration.setVisibility(View.VISIBLE);
                this.shortcut.setVisibility(View.VISIBLE);
            }
        }
    }
}