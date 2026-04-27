package com.whisperyao.dsplayer.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.databinding.ContainerGridItemBinding;
import com.whisperyao.dsplayer.databinding.ContainerListItemBinding;
import com.whisperyao.dsplayer.fragment.ContainerFragment;
import com.whisperyao.dsplayer.item.Item;
import java.io.File;
import java.util.List;

public class ContainerAdapter extends AbstractContainerAdapter {
    public ContainerAdapter(ContainerFragment f) {
        super(f);
    }

    @Override
    public void setData(List<Item> data) {
        super.setData(data);
        if (data != null) {
            calCoverPath(data);
        }
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(parent.getContext());
        if (2 == viewType) {
            ContainerGridItemBinding containerGridItemBindingInflate = ContainerGridItemBinding.inflate(layoutInflaterFrom, parent, false);
            containerGridItemBindingInflate.getRoot().setOnClickListener(this);
            return new GridHolder(containerGridItemBindingInflate);
        }
        ContainerListItemBinding containerListItemBindingInflate = ContainerListItemBinding.inflate(layoutInflaterFrom, parent, false);
        containerListItemBindingInflate.getRoot().setOnClickListener(this);
        return new ListHolder(containerListItemBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, Item song, int position) {
        if (holder instanceof GridHolder) {
            holder.showData(this.data.get(position));
        }
        if (holder instanceof ListHolder) {
            holder.showData(this.data.get(position));
        }
    }

    class GridHolder extends AbsHolder<Item> {
        private final SimpleDraweeView cover;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView title;

        GridHolder(ContainerGridItemBinding binding) {
            super(binding.getRoot());
            this.title = binding.title;
            this.subtitle = binding.subtitle;
            this.cover = binding.cover;
            this.shortcut = binding.shortcut;
        }

        @Override
        public void showData(Item item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(ContainerAdapter.this);
            this.title.setText(item.getTitle());
            if (TextUtils.isEmpty(item.getDisplayArtist())) {
                this.subtitle.setVisibility(4);
            } else {
                this.subtitle.setVisibility(0);
                this.subtitle.setText(item.getDisplayArtist());
            }
            if (item.isAllSongs()) {
                if (Common.ContainerType.GENRE_ARTIST_MODE.equals(ContainerAdapter.this.mType)) {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_artist).build().getSourceUri());
                } else {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_all).build().getSourceUri());
                }
            } else {
                ContainerAdapter.this.showContainerCover(this.cover, item);
            }
            if (ContainerAdapter.this.isCheckMode()) {
                this.shortcut.setVisibility(8);
            } else {
                this.shortcut.setVisibility(0);
            }
        }
    }

    class ListHolder extends AbsHolder<Item> {
        private final SimpleDraweeView cover;
        private final CheckBox mark;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView title;

        ListHolder(ContainerListItemBinding binding) {
            super(binding.getRoot());
            CheckBox checkBox = binding.checkbox;
            this.mark = checkBox;
            checkBox.setVisibility(8);
            this.shortcut = binding.shortcut;
            this.title = binding.title;
            this.subtitle = binding.subtitle;
            this.cover = binding.cover;
        }

        @Override
        public void showData(Item item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(ContainerAdapter.this);
            if (!StateManager.getInstance().isMobileLayout() && ContainerAdapter.this.isLeft() && ContainerAdapter.this.selPos == getAdapterPosition()) {
                this.itemView.setBackgroundResource(R.color.list_press_over_light);
            } else {
                this.itemView.setBackgroundResource(R.color.transparent);
            }
            this.title.setText(item.getTitle());
            if (TextUtils.isEmpty(item.getDisplayArtist())) {
                this.subtitle.setVisibility(8);
            } else {
                this.subtitle.setVisibility(0);
                this.subtitle.setText(item.getDisplayArtist());
            }
            if (item.isAllSongs()) {
                if (ContainerAdapter.this.mType.equals(Common.ContainerType.GENRE_ARTIST_MODE)) {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_artist).build().getSourceUri());
                } else {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_all).build().getSourceUri());
                }
            } else {
                ContainerAdapter.this.showContainerCover(this.cover, item);
            }
            if (ContainerAdapter.this.isCheckMode()) {
                this.mark.setVisibility(0);
                this.shortcut.setVisibility(8);
                this.mark.setChecked(item.isMarked());
            } else {
                this.mark.setVisibility(8);
                this.shortcut.setVisibility(0);
            }
        }
    }

    private void showContainerCover(SimpleDraweeView cover, Item item) {
        String coverPath;
        if (!this.mIsOnline && (coverPath = getCoverPath(item)) != null && !coverPath.isEmpty()) {
            cover.setImageRequest(ImageRequest.fromFile(new File(coverPath)));
        } else {
            new CoverUriLoader().with(cover).containerType(this.mType).placeHolder(this.mType).load(this.mContainerFragment.getEnumSongsBundle(item));
        }
    }
}
