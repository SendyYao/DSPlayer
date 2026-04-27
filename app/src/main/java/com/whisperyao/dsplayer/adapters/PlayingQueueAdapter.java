package com.whisperyao.dsplayer.adapters;

import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.databinding.PlayingqItemBinding;
import com.whisperyao.dsplayer.item.SongItem;
import java.util.ArrayList;

public class PlayingQueueAdapter extends AbsAdapter<SongItem> {
    private int backupQueueId;
    private boolean isPlaying;
    private final ArrayList<Integer> mHoldSongIndex;
    private int nowPlayingQueueId;

    public boolean setNowPlayingQueueId(int id) {
        boolean z;
        if (isDragMode()) {
            if (id >= 0 && id < this.mHoldSongIndex.size()) {
                id = this.mHoldSongIndex.indexOf(Integer.valueOf(id));
            }
            z = false;
        } else {
            z = true;
        }
        boolean z2 = this.nowPlayingQueueId != id;
        this.nowPlayingQueueId = id;
        notifyDataSetChanged();
        return z2 && z;
    }

    public void setPlayingStatus(PlaybackStateCompat state) {
        boolean z = state != null && (state.getState() == 3 || state.getState() == 6);
        if (this.isPlaying != z) {
            this.isPlaying = z;
            notifyItemChanged(this.nowPlayingQueueId);
        }
    }

    @Override
    public void setIsDragMode(boolean b) {
        if (b) {
            this.backupQueueId = this.nowPlayingQueueId;
        }
        super.setIsDragMode(b);
    }

    public PlayingQueueAdapter(AbsAdapter.Callback c) {
        super(c);
        this.mHoldSongIndex = new ArrayList<>();
        this.isPlaying = false;
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PlayingqItemBinding playingqItemBindingInflate = PlayingqItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        playingqItemBindingInflate.getRoot().setOnClickListener(this);
        return new ListHolder(playingqItemBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, SongItem song, int position) {
        if (holder instanceof ListHolder) {
            ((ListHolder) holder).showData((SongItem) this.data.get(position));
        }
    }

    public int[] getSelectedPositions() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mCheckedList.length; i++) {
            if (this.mCheckedList[i]) {
                arrayList.add(i);
            }
        }
        int size = arrayList.size();
        int[] iArr = new int[size];
        for (int i2 = 0; i2 < size; i2++) {
            iArr[i2] = arrayList.get(i2);
        }
        return iArr;
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
        if (fromPosition == -1 || toPosition == -1) {
            return;
        }
        int i = this.nowPlayingQueueId;
        if (fromPosition == i) {
            this.nowPlayingQueueId = toPosition;
        } else if (toPosition == i) {
            if (fromPosition - toPosition > 0) {
                this.nowPlayingQueueId = i + 1;
            } else {
                this.nowPlayingQueueId = i - 1;
            }
        }
        super.onMove(fromPosition, toPosition);
        if (this.mHoldSongIndex.isEmpty()) {
            for (int i2 = 0; i2 < this.data.size(); i2++) {
                this.mHoldSongIndex.add(i2, i2);
            }
        }
        this.mHoldSongIndex.add(toPosition, this.mHoldSongIndex.remove(fromPosition));
    }

    public ArrayList<Integer> getHoldSongIndex() {
        return this.mHoldSongIndex;
    }

    @Override
    public void applyOrder() {
        this.mHoldSongIndex.clear();
        super.applyOrder();
    }

    @Override
    public void undoOrder() {
        this.mHoldSongIndex.clear();
        this.nowPlayingQueueId = this.backupQueueId;
        super.undoOrder();
    }

    class ListHolder extends AbsHolder<SongItem> {
        private final ImageView drag;
        private final TextView duration;
        private final ImageView icon;
        private final LinearLayout layout;
        private final CheckBox mark;
        private final int playing_bg;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView title;
        private final int transparent;

        ListHolder(PlayingqItemBinding binding) {
            super(binding.getRoot());
            this.layout = binding.SongItemLayout;
            this.title = binding.SongItemTitle;
            this.subtitle = binding.SongItemSubTitle;
            this.duration = this.itemView.findViewById(R.id.SongItemTime);
            this.icon = binding.SongItemIcon;
            this.mark = binding.SongItemCheckBox;
            this.shortcut = binding.SongItemShortCut;
            ImageView imageView = binding.dragHandle;
            this.drag = imageView;
            imageView.setOnTouchListener((view, motionEvent) -> {
                if (PlayingQueueAdapter.this.touchHelper == null) {
                    return true;
                }
                PlayingQueueAdapter.this.touchHelper.startDrag(this);
                return true;
            });
            this.playing_bg = this.itemView.getContext().getResources().getColor(R.color.playing_bg);
            this.transparent = this.itemView.getContext().getResources().getColor(R.color.transparent);
        }

        @Override
        public void showData(SongItem item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(PlayingQueueAdapter.this);
            this.mark.setTag(getAdapterPosition());
            this.mark.setOnClickListener(PlayingQueueAdapter.this);
            this.title.setText(item.getTitle());
            if (item.isFile()) {
                this.subtitle.setText(item.getSongDescription());
            } else {
                this.subtitle.setText(R.string.str_internet_radio);
            }
            TextView textView = this.duration;
            if (textView != null) {
                textView.setVisibility(0);
                if (item.isFile()) {
                    this.duration.setText(item.getTimeString());
                } else {
                    this.duration.setText("--:--");
                }
            }
            if (PlayingQueueAdapter.this.isPlaying && PlayingQueueAdapter.this.nowPlayingQueueId == getAdapterPosition()) {
                this.icon.setImageResource(R.drawable.icon_play);
                this.layout.setBackgroundColor(this.playing_bg);
            } else {
                this.icon.setImageResource(R.drawable.icon_music);
                this.layout.setBackgroundColor(this.transparent);
            }
            if (PlayingQueueAdapter.this.isDragMode()) {
                this.drag.setVisibility(0);
                this.mark.setVisibility(8);
                this.shortcut.setVisibility(8);
                TextView textView2 = this.duration;
                if (textView2 != null) {
                    textView2.setVisibility(8);
                    return;
                }
                return;
            }
            if (PlayingQueueAdapter.this.isCheckMode()) {
                this.drag.setVisibility(8);
                this.shortcut.setVisibility(8);
                this.mark.setVisibility(0);
                this.mark.setChecked(PlayingQueueAdapter.this.isItemChecked(getAdapterPosition()));
                return;
            }
            this.drag.setVisibility(8);
            this.mark.setVisibility(8);
            this.shortcut.setVisibility(0);
        }
    }
}
