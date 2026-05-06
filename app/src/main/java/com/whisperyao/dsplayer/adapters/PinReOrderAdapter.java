package com.whisperyao.dsplayer.adapters;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.databinding.PinItemDragMultiselectBinding;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.HomePagePinItem;
import com.whisperyao.dsplayer.item.PlaylistItem;

import java.util.ArrayList;

public class PinReOrderAdapter extends AbsAdapter<HomePagePinItem> {
    private final ArrayList<HomePagePinItem> removedItem;

    public PinReOrderAdapter(AbsAdapter.Callback c) {
        super(c);
        this.removedItem = new ArrayList<>();
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PinItemDragMultiselectBinding pinItemDragMultiselectBindingInflate = PinItemDragMultiselectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        pinItemDragMultiselectBindingInflate.getRoot().setOnClickListener(this);
        return new ListHolder(pinItemDragMultiselectBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, HomePagePinItem song, int position) {
        if (holder instanceof ListHolder) {
            holder.showData(this.data.get(position));
        }
    }

    public ArrayList<HomePagePinItem> getSelectedItems() {
        ArrayList<HomePagePinItem> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mCheckedList.length; i++) {
            if (this.mCheckedList[i]) {
                arrayList.add(this.data.get(i));
            }
        }
        return arrayList;
    }

    public ArrayList<HomePagePinItem> removeSelectedItem() {
        for (int realItemCount = getRealItemCount() - 1; realItemCount >= 0; realItemCount--) {
            if (this.mCheckedList[realItemCount]) {
                this.removedItem.add(this.data.remove(realItemCount));
            }
        }
        this.mCheckedList = new boolean[getRealItemCount()];
        notifyDataSetChanged();
        return this.removedItem;
    }

    public ArrayList<HomePagePinItem> getReOrdedSet() {
        return this.data;
    }

    @Override
    public void applyOrder() {
        super.applyOrder();
    }

    @Override
    public void undoOrder() {
        super.undoOrder();
    }

    @Override
    public void onClick(View v) {
        int realDataPosition = getRealDataPosition((Integer) v.getTag());
        int iIntValue = (Integer) v.getTag();
        if (this.mOnItemClickListener != null) {
            checkItem(iIntValue);
            notifyItemChanged(iIntValue);
            this.mOnItemClickListener.onItemClick(v, this.data.get(realDataPosition), realDataPosition);
        }
    }

    @Override
    public void onMove(int fromPosition, int toPosition) {
        super.onMove(fromPosition, toPosition);
        if (this.callback != null) {
            this.callback.onTrackOrderChanged(0);
        }
    }

    class ListHolder extends AbsHolder<HomePagePinItem> {
        private final SimpleDraweeView cover;
        private final ImageView mark;
        private final TextView title;
        @SuppressLint("ClickableViewAccessibility")
        ListHolder(PinItemDragMultiselectBinding binding) {
            super(binding.getRoot());
            this.cover = binding.PinItemCover;
            this.mark = binding.PinItemMark;
            this.title = binding.PinItemTitle;
            ImageView imageView = binding.dragHandle;
            imageView.setOnTouchListener((view, event) -> {
                if (PinReOrderAdapter.this.touchHelper == null) {
                    return true;
                }
                PinReOrderAdapter.this.touchHelper.startDrag(ListHolder.this);
                return true;
            });
        }

        @Override
        public void showData(HomePagePinItem item) {
            this.title.setText(item.getTitle());
            Bundle enumSongsBundle = PinManager.Companion.getEnumSongsBundle(item);
            Common.ContainerType containerTypeByItem = PinManager.Companion.getContainerTypeByItem(item);
            if (item.getType().equals("folder")) {
                new CoverUriLoader().with(this.cover).placeHolder(containerTypeByItem).load(enumSongsBundle);
            } else if (item.getType().equals(PinManager.TYPE_RANDOM_100)) {
                this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_100).build().getSourceUri());
            } else if (item.getType().equals("playlist")) {
                PlaylistItem playlistItemFromBundle = PlaylistItem.Companion.fromBundle(enumSongsBundle.getBundle(PinManager.EXTRA_PLAYLIST));
                if (playlistItemFromBundle.isSharedSong()) {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_shared).build().getSourceUri());
                } else if (playlistItemFromBundle.isNormal()) {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_playlist).build().getSourceUri());
                } else {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_smart_playlist).build().getSourceUri());
                }
            } else if (item.getType().equals(PinManager.TYPE_RECENTLY_ADDED)) {
                this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_recently_add).build().getSourceUri());
            } else {
                new CoverUriLoader().with(this.cover).placeHolder(containerTypeByItem).load(enumSongsBundle);
            }
            PinReOrderAdapter pinReOrderAdapter = PinReOrderAdapter.this;
            if (pinReOrderAdapter.isItemChecked(pinReOrderAdapter.getRealDataPosition(getItemViewPosition()))) {
                this.mark.setVisibility(View.VISIBLE);
                this.cover.setBackgroundResource(R.color.colorPrimary);
                this.cover.setAlpha(64);
                this.itemView.setBackgroundResource(R.color.selected_bg_color);
                return;
            }
            this.mark.setVisibility(View.GONE);
            this.cover.setBackgroundResource(R.color.white);
            this.cover.setAlpha(255);
            this.itemView.setBackgroundResource(R.drawable.transparent);
        }
    }
}
