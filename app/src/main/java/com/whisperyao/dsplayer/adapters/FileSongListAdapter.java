package com.whisperyao.dsplayer.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.databinding.ContainerGridItemBinding;
import com.whisperyao.dsplayer.fragment.FileSongFragment;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;

public class FileSongListAdapter extends AbsAdapter<SongItem> {
    private final int VIEW_TYPE_GRID_FILE_WIDE;
    private final int VIEW_TYPE_GRID_FOLDER;
    private final int VIEW_TYPE_LIST_FILE_NARROW;
    private final int VIEW_TYPE_LIST_FILE_WIDE;
    private final int VIEW_TYPE_LIST_FOLDER;
    private final int VIEW_TYPE_SEGMENT;
    private View mViewSegment;
    private boolean showDuration;
    private boolean showSegment;

    public FileSongListAdapter(AbsAdapter.Callback c) {
        super(c);
        this.VIEW_TYPE_SEGMENT = -2;
        this.VIEW_TYPE_LIST_FOLDER = 0;
        this.VIEW_TYPE_LIST_FILE_NARROW = 1;
        this.VIEW_TYPE_LIST_FILE_WIDE = 2;
        this.VIEW_TYPE_GRID_FOLDER = 3;
        this.VIEW_TYPE_GRID_FILE_WIDE = 5;
        this.showDuration = !StateManager.getInstance().isMobileLayout() && !isLeft();
    }

    public void setSegment(View view) {
        this.showSegment = true;
        this.mViewSegment = view;
        checkDividers();
        notifyDataSetChanged();
    }

    public void disableSegment() {
        this.showSegment = false;
        this.mViewSegment = null;
        checkDividers();
        notifyDataSetChanged();
    }

    private void applyRoundCorner(SimpleDraweeView view) {
        RoundingParams roundingParams =
                RoundingParams.fromCornersRadius(16f);

        GenericDraweeHierarchy hierarchy = view.getHierarchy();
        hierarchy.setRoundingParams(roundingParams);
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SynoLog.d("FileSongListAdapter", "viewType: " + viewType);
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(parent.getContext());
        if (3 == viewType || 4 == viewType || 5 == viewType) {
            ContainerGridItemBinding containerGridItemBindingInflate = ContainerGridItemBinding.inflate(layoutInflaterFrom, parent, false);
            containerGridItemBindingInflate.getRoot().setOnClickListener(this);
            return new GridHolder(containerGridItemBindingInflate);
        }
        if (viewType == 0) {
            View viewInflate = layoutInflaterFrom.inflate(R.layout.container_list_item, parent, false);
            viewInflate.setOnClickListener(this);
            return new ListFolderHolder(viewInflate);
        }
        if (viewType == 1) {
            View viewInflate2 = layoutInflaterFrom.inflate(R.layout.song_item_fresco, parent, false);
            viewInflate2.setOnClickListener(this);
            return new ListFileNarrowHolder(viewInflate2);
        }
        if (viewType == 2) {
            View viewInflate3 = layoutInflaterFrom.inflate(R.layout.tablet_song_item_fresco, parent, false);
            viewInflate3.setOnClickListener(this);
            return new ListFileWideHolder(viewInflate3);
        }
        if (viewType == -2) {
            return new HeaderHolder(this.mViewSegment);
        }
        return new HeaderHolder(getHeader());
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
    public int getItemViewType(int position) {
        if (this.showSegment && isShowHeader()) {
            if (position == 0) {
                return -2;
            }
            if (position == 1) {
                return -1;
            }
        }
        if (position == 0) {
            if (this.showSegment) {
                return -2;
            }
            if (isShowHeader()) {
                return -1;
            }
        }
        return this.data.get(getRealDataPosition(position)).getType().isDirectory() ? isListMode() ? 0 : 3 : StateManager.getInstance().isMobileLayout() ? isListMode() ? 1 : 4 : isListMode() ? 2 : 5;
    }

    @Override
    protected final int getRealDataPosition(int i) {
        return (isShowHeader() && this.showSegment) ? i - 2 : (isShowHeader() || this.showSegment) ? i - 1 : i;
    }

    @Override
    public int getItemCount() {
        int i;
        if (isShowHeader() && this.showSegment) {
            i = 2;
        } else {
            i = (isShowHeader() || this.showSegment) ? 1 : 0;
        }
        return getRealItemCount() + i;
    }

    @Override
    public void onBind(AbsHolder holder, SongItem song, int position) {
        if (holder instanceof ListHolder) {
            holder.showData(this.data.get(position));
        }
        if (holder instanceof GridHolder) {
            holder.showData(this.data.get(position));
        }
    }

    @Override
    protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (FileSongListAdapter.this.isShowHeader() && FileSongListAdapter.this.showSegment && position <= 1) {
                    return FileSongListAdapter.this.getSpan();
                }
                if (FileSongListAdapter.this.isShowHeader() || (FileSongListAdapter.this.showSegment && position == 0)) {
                    return FileSongListAdapter.this.getSpan();
                }
                if (FileSongListAdapter.this.isListMode()) {
                    return FileSongListAdapter.this.getSpan();
                }
                return 1;
            }
        };
    }

    @Override
    protected int getHeaderOffset() {
        if (isShowHeader() && this.showSegment) {
            return 2;
        }
        return (isShowHeader() || this.showSegment) ? 1 : 0;
    }

    class ListHolder extends AbsHolder<SongItem> {
        TextView album;
        TextView artist;
        SimpleDraweeView cover;
        TextView duration;
        ImageView icon;
        CheckBox mark;
        ImageView shortcut;
        TextView subtitle;
        TextView title;

        ListHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void showData(SongItem item) {
            SynoLog.d("FileSongListAdapter", "songItem: " + item.getTitle());
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(FileSongListAdapter.this);
            this.title.setText(item.getTitle());
            new CoverUriLoader().with(this.cover).placeHolder(Common.ContainerType.ALBUM_MODE).load(item);
            TextView textView = this.duration;
            if (textView != null) {
                textView.setText(item.getTimeString());
            }
            TextView textView2 = this.subtitle;
            if (textView2 != null) {
                textView2.setText(item.getSongDescription());
            }
            TextView textView3 = this.album;
            if (textView3 != null) {
                textView3.setText(item.getAlbum());
            }
            TextView textView4 = this.artist;
            if (textView4 != null) {
                textView4.setText(item.getArtist());
            }
            if (FileSongListAdapter.this.isCheckMode()) {
                this.shortcut.setVisibility(View.GONE);
                if (item.getType().isDirectory()) {
                    this.mark.setVisibility(View.GONE);
                } else {
                    this.mark.setChecked(FileSongListAdapter.this.isItemChecked(getAdapterPosition()));
                    this.mark.setVisibility(View.VISIBLE);
                }
                return;
            }
            this.mark.setVisibility(View.GONE);
            this.shortcut.setVisibility(View.VISIBLE);
        }
    }

    class ListFolderHolder extends ListHolder {
        ListFolderHolder(View itemView) {
            super(itemView);
            this.mark = itemView.findViewById(R.id.checkbox);
            this.shortcut = itemView.findViewById(R.id.shortcut);
            this.title = itemView.findViewById(R.id.title);
            this.cover = itemView.findViewById(R.id.cover);
            applyRoundCorner(this.cover);
            this.mark.setVisibility(View.GONE);
        }

        @Override
        public void showData(SongItem item) {
            this.title.setText(item.getTitle());
            this.cover.setImageResource(R.drawable.border);
            new CoverUriLoader().with(this.cover).placeHolder(Common.ContainerType.FOLDER_MODE).load(((FileSongFragment) FileSongListAdapter.this.callback).getEnumSongsBundle(item));
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(FileSongListAdapter.this);
            if (FileSongListAdapter.this.isCheckMode()) {
                this.shortcut.setVisibility(View.GONE);
                if (item.getType().isDirectory()) {
                    this.mark.setVisibility(View.GONE);
                } else {
                    this.mark.setVisibility(View.VISIBLE);
                }
                return;
            }
            this.mark.setVisibility(View.GONE);
            this.shortcut.setVisibility(View.VISIBLE);
        }
    }

    class ListFileNarrowHolder extends ListHolder {
        ListFileNarrowHolder(View itemView) {
            super(itemView);
            this.mark = itemView.findViewById(R.id.SongItemCheckBox);
            this.shortcut = itemView.findViewById(R.id.SongItemShortCut);
            this.title = itemView.findViewById(R.id.SongItemTitle);
            this.subtitle = itemView.findViewById(R.id.SongItemSubTitle);
            this.duration = itemView.findViewById(R.id.SongItemTime);
            this.icon = itemView.findViewById(R.id.SongItemIcon);
            this.icon.setImageResource(R.drawable.icon_music);
            this.icon.setVisibility(View.GONE);
            this.cover = itemView.findViewById(R.id.SongItemCover);
            applyRoundCorner(this.cover);
            this.cover.setVisibility(View.VISIBLE);
            this.duration.setVisibility(FileSongListAdapter.this.showDuration ? View.VISIBLE : View.GONE);
            this.subtitle.setVisibility(View.VISIBLE);
        }
    }

    class ListFileWideHolder extends ListHolder {
        ListFileWideHolder(View itemView) {
            super(itemView);
            this.mark = itemView.findViewById(R.id.SongItemCheckBox);
            this.shortcut = itemView.findViewById(R.id.SongItemShortCut);
            this.album = itemView.findViewById(R.id.SongItemAlbum);
            this.artist = itemView.findViewById(R.id.SongItemArtist);
            this.title = itemView.findViewById(R.id.SongItemTitle);
            this.duration = itemView.findViewById(R.id.SongItemTime);
            this.icon = itemView.findViewById(R.id.SongItemIcon);
            this.cover = itemView.findViewById(R.id.SongItemCover);
            applyRoundCorner(this.cover);
            this.cover.setVisibility(View.VISIBLE);
            this.duration.setVisibility(FileSongListAdapter.this.showDuration ? View.VISIBLE : View.GONE);
            this.album.setVisibility(View.VISIBLE);
            this.artist.setVisibility(View.VISIBLE);
        }
    }

    class GridHolder extends AbsHolder<SongItem> {
        private final SimpleDraweeView cover;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView title;

        GridHolder(ContainerGridItemBinding binding) {
            super(binding.getRoot());
            this.title = binding.title;
            this.subtitle = binding.subtitle;
            this.cover = binding.cover;
            applyRoundCorner(this.cover);
            this.shortcut = binding.shortcut;
        }

        @Override
        public void showData(SongItem item) {
            SynoLog.d("FileSongListAdapter", "songItem: " + item.toJsonString());
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(FileSongListAdapter.this);
            this.title.setText(item.getTitle());
            if (TextUtils.isEmpty(item.getDisplayArtist())) {
                this.subtitle.setVisibility(View.INVISIBLE);
            } else {
                this.subtitle.setVisibility(View.VISIBLE);
                this.subtitle.setText(item.getDisplayArtist());
            }
            new CoverUriLoader().with(this.cover).placeHolder(Common.ContainerType.FOLDER_MODE).load(((FileSongFragment) FileSongListAdapter.this.callback).getEnumSongsBundle(item));
        }
    }

    class HeaderHolder extends AbsHolder {
        @Override
        public void showData(Object o) {
        }

        HeaderHolder(View itemView) {
            super(itemView);
        }
    }
}
