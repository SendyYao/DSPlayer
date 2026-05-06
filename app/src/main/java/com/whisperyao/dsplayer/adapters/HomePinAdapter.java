package com.whisperyao.dsplayer.adapters;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.databinding.ContainerGridItemBinding;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.HomePagePinItem;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.HashMap;

public class HomePinAdapter extends AbsAdapter<HomePagePinItem> {
    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ContainerGridItemBinding containerGridItemBindingInflate = ContainerGridItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        containerGridItemBindingInflate.getRoot().setOnClickListener(this);
        return new GridHolder(containerGridItemBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, HomePagePinItem song, int position) {
        System.out.println("HomePinAdapter Bind");
        if (holder instanceof GridHolder) {
            holder.showData(this.data.get(position));
        }
    }

    class GridHolder extends AbsHolder<HomePagePinItem> {
        private final SimpleDraweeView cover;
        private final ImageView pinIcon;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView title;

        GridHolder(ContainerGridItemBinding binding) {
            super(binding.getRoot());
            this.title = binding.title;
            this.subtitle = binding.subtitle;
            SimpleDraweeView simpleDraweeView = binding.cover;
            this.cover = simpleDraweeView;
            simpleDraweeView.getHierarchy().setPlaceholderImage(R.drawable.thumbnail_song);
            this.shortcut = binding.shortcut;
            ImageView imageView = binding.pinIcon;
            this.pinIcon = imageView;
            imageView.setVisibility(View.VISIBLE);
        }

        @Override
        public void showData(HomePagePinItem item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(HomePinAdapter.this);
            this.title.setText(item.getTitle());
            this.subtitle.setVisibility(View.INVISIBLE);
            HashMap<String, String> criteria = item.getCriteria();
            if (!TextUtils.isEmpty(criteria.get("album_artist"))) {
                this.subtitle.setVisibility(View.VISIBLE);
                this.subtitle.setText(criteria.get("album_artist"));
            } else if (!TextUtils.isEmpty(criteria.get(Common.SearchCategory.ARTIST))) {
                this.subtitle.setVisibility(View.VISIBLE);
                this.subtitle.setText(criteria.get(Common.SearchCategory.ARTIST));
            }
            Bundle enumSongsBundle = PinManager.getEnumSongsBundle(item);
            Common.ContainerType containerTypeByItem = PinManager.getContainerTypeByItem(item);
            this.cover.setImageResource(R.drawable.border);
            SynoLog.d("HomePinAdapter", item.getType().toString());
            if (item.getType().equals("folder")) {
                new CoverUriLoader().with(this.cover).placeHolder(containerTypeByItem).load(enumSongsBundle);
            } else if (item.getType().equals(PinManager.TYPE_RANDOM_100)) {
                this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_100).build().getSourceUri());
            }
            else if (item.getType().equals("playlist")) {
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
            this.pinIcon.setImageResource(HomePinAdapter.this.getPinIconRes(item));
        }
    }

    public int getPinIconRes(com.whisperyao.dsplayer.item.HomePagePinItem item) {
        if (item == null || item.getType() == null) {
            return -1;
        }

        String type = item.getType();

        switch (type) {
            case "artist":
                return R.drawable.icon_pin_artist;

            case "folder":
                return R.drawable.icon_pin_folder;

            case "composer":
                return R.drawable.icon_pin_composer;

            case "album":
                return R.drawable.icon_pin_album;

            case "genre":
                return R.drawable.icon_pin_genre;

            case "random_100":
                return R.drawable.icon_pin_random100;

            case "recently_added":
                return R.drawable.icon_pin_recently_added;

            case "playlist":
                return R.drawable.icon_pin_playlist;

            default:
                return -1;
        }
    }
}