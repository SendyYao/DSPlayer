package com.whisperyao.dsplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.databinding.ContainerListItemBinding;
import com.whisperyao.dsplayer.fragment.RatingFragment;

public class RatingAdapter extends AbsAdapter<RatingFragment.RatingLevelItem> {
    public RatingAdapter() {
        this.data.add(new RatingFragment.RatingLevelItem(5));
        this.data.add(new RatingFragment.RatingLevelItem(4));
        this.data.add(new RatingFragment.RatingLevelItem(3));
        this.data.add(new RatingFragment.RatingLevelItem(2));
        this.data.add(new RatingFragment.RatingLevelItem(1));
        this.data.add(new RatingFragment.RatingLevelItem(0));
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ContainerListItemBinding containerListItemBindingInflate = ContainerListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        containerListItemBindingInflate.getRoot().setOnClickListener(this);
        return new RadioHolder(containerListItemBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, RatingFragment.RatingLevelItem song, int position) {
        if (holder instanceof RadioHolder) {
            holder.showData(song);
        }
    }

    public int getCount() {
        return this.data.size();
    }

    public RatingFragment.RatingLevelItem getItem(int position) {
        return this.data.get(getRealDataPosition(position));
    }

    class RadioHolder extends AbsHolder<RatingFragment.RatingLevelItem> {
        private final SimpleDraweeView mCoverView;
        private final TextView mTitleView;
        private final ImageView shortcut;

        RadioHolder(ContainerListItemBinding binding) {
            super(binding.getRoot());
            this.mCoverView = binding.cover;
            this.mTitleView = binding.title;
            this.shortcut = binding.shortcut;
            binding.checkbox.setVisibility(View.GONE);
        }

        @Override
        public void showData(RatingFragment.RatingLevelItem item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(RatingAdapter.this);
            this.mCoverView.setImageResource(item.getIconResId());
            this.mTitleView.setText(item.getTitleId());
            if (!StateManager.getInstance().isMobileLayout() && RatingAdapter.this.isLeft() && RatingAdapter.this.selPos == getAdapterPosition()) {
                this.itemView.setBackgroundResource(R.color.list_press_over_light);
            } else {
                this.itemView.setBackgroundResource(R.color.transparent);
            }
        }
    }
}
