package com.whisperyao.dsplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.databinding.ContainerListItemBinding;
import com.whisperyao.dsplayer.databinding.SongItemFrescoBinding;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;

public class RadioListAdapter extends AbsAdapter<SongItem> {
    private static final String API_FavoriteID = "Favorite";
    private static final String API_UserDefinedID = "UserDefined";
    private static final String CGI_FavoriteID = "inetradio_favorite";
    private static final String CGI_UserDefinedID = "inetradio_userdefined";
    private static final int VIEW_TYPE_RADIO = 1;
    private static final int VIEW_TYPE_RADIO_FOLDER = 0;

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            ContainerListItemBinding containerListItemBindingInflate = ContainerListItemBinding.inflate(layoutInflaterFrom, parent, false);
            containerListItemBindingInflate.getRoot().setOnClickListener(this);
            return new RadioFolderHolder(containerListItemBindingInflate);
        }
        SongItemFrescoBinding songItemFrescoBindingInflate = SongItemFrescoBinding.inflate(layoutInflaterFrom, parent, false);
        songItemFrescoBindingInflate.getRoot().setOnClickListener(this);
        return new RadioHolder(songItemFrescoBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, SongItem song, int position) {
        if (holder instanceof RadioFolderHolder) {
            holder.showData(song);
        }
        if (holder instanceof RadioHolder) {
            holder.showData(song);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return Item.ItemType.CONTAINER_MODE.equals(this.data.get(position).getType()) ? 0 : 1;
    }

    class RadioFolderHolder extends AbsHolder<SongItem> {
        private final SimpleDraweeView cover;
        private final TextView title;

        RadioFolderHolder(ContainerListItemBinding binding) {
            super(binding.getRoot());
            this.title = binding.title;
            this.cover = binding.cover;
            binding.subtitle.setVisibility(View.GONE);
            binding.checkbox.setVisibility(View.GONE);
            binding.shortcut.setVisibility(View.GONE);
        }

        @Override
        public void showData(SongItem item) {
            this.title.setText(item.getTitle());
            if ("inetradio_favorite".equals(item.getID()) || RadioListAdapter.API_FavoriteID.equals(item.getID())) {
                this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_fav).build().getSourceUri());
                this.title.setText(this.itemView.getContext().getResources().getString(R.string.str_my_favorate));
            } else {
                if ("inetradio_userdefined".equals(item.getID()) || RadioListAdapter.API_UserDefinedID.equals(item.getID())) {
                    this.cover.setImageURI(ImageRequestBuilder.newBuilderWithResourceId(R.drawable.thumbnail_userdefind).build().getSourceUri());
                    this.title.setText(this.itemView.getContext().getResources().getString(R.string.str_user_defined));
                    return;
                }
                this.cover.setImageResource(R.drawable.thumbnail_radiofolder);
            }
        }
    }

    class RadioHolder extends AbsHolder<SongItem> {
        private final ImageView icon;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView title;

        RadioHolder(SongItemFrescoBinding binding) {
            super(binding.getRoot());
            this.title = binding.SongItemTitle;
            this.icon = binding.SongItemIcon;
            TextView textView = binding.SongItemSubTitle;
            this.subtitle = textView;
            ImageView imageView = binding.SongItemShortCut;
            this.shortcut = imageView;
            binding.SongItemTime.setVisibility(View.GONE);
            binding.SongItemCover.setVisibility(View.GONE);
            binding.SongItemCheckBox.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        }

        @Override
        public void showData(SongItem item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(RadioListAdapter.this);
            this.title.setText(item.getTitle());
            this.icon.setImageResource(R.drawable.icon_radio);
            this.subtitle.setText(item.getFilePath());
        }
    }
}
