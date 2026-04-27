package com.whisperyao.dsplayer.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.databinding.ContainerGridItemBinding;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.net.AudioStationAPI;


public class DefaultGenreAdapter extends AbsAdapter<Item> {
    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ContainerGridItemBinding containerGridItemBindingInflate = ContainerGridItemBinding
                .inflate(LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );
        containerGridItemBindingInflate.getRoot().setOnClickListener(this);
        return new GridHolder(containerGridItemBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, Item song, int position) {
        if (holder instanceof GridHolder) {
            holder.showData(this.data.get(position));
        }
    }

    class GridHolder extends AbsHolder<Item> {
        private final SimpleDraweeView cover;
        private final TextView subtitle;
        private final TextView title;

        GridHolder(ContainerGridItemBinding binding) {
            super(binding.getRoot());
            this.title = binding.title;
            this.subtitle = binding.subtitle;
            this.cover = binding.cover;
            binding.shortcut.setVisibility(View.GONE);
        }

        @Override
        public void showData(Item item) {
            this.title.setText(item.getTitle());
            if (TextUtils.isEmpty(item.getDisplayArtist())) {
                this.subtitle.setVisibility(View.INVISIBLE);
            } else {
                this.subtitle.setVisibility(View.VISIBLE);
                this.subtitle.setText(item.getDisplayArtist());
            }
            if (item.isAllSongs()) {
                this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_all).build().getSourceUri());
            } else {
                new CoverUriLoader()
                        .with(this.cover)
                        .placeHolder(Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE)
                        .load(item, AudioStationAPI.SYNO_AUDIOSTATION_COVER);
            }
        }
    }
}
