package com.whisperyao.dsplayer.adapters;

import android.content.res.Configuration;
import android.net.Uri;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.databinding.DownloadItemBinding;
import com.whisperyao.dsplayer.download.TaskManager;
import com.whisperyao.dsplayer.item.SongItem;

public class TaskAdapter extends AbsAdapter<SongItem> {
    private final TaskManager taskManager;

    public TaskAdapter(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public AbsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DownloadItemBinding downloadItemBindingInflate = DownloadItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        downloadItemBindingInflate.getRoot().setOnClickListener(this);
        return new TaskHolder(downloadItemBindingInflate);
    }

    @Override
    public void onBind(AbsHolder holder, SongItem song, int position) {
        if (holder instanceof TaskHolder) {
            holder.showData(this.data.get(position));
        }
    }

    class TaskHolder extends AbsHolder<SongItem> {
        private final TextView album;
        private final TextView artist;
        private final SimpleDraweeView cover;
        private final ProgressBar progressbar;
        private final ImageView shortcut;
        private final TextView subtitle;
        private final TextView time;
        private final TextView title;

        TaskHolder(DownloadItemBinding binding) {
            super(binding.getRoot());
            this.album = this.itemView.findViewById(R.id.SongItemAlbum);
            this.subtitle = this.itemView.findViewById(R.id.SongItemSubTitle);
            this.artist = this.itemView.findViewById(R.id.SongItemArtist);
            this.title = binding.SongItemTitle;
            this.time = binding.SongItemTime;
            this.cover = binding.SongItemCover;
            this.shortcut = binding.SongItemShortCut;
            this.progressbar = binding.SongItemProgress;
        }

        @Override
        public void showData(SongItem item) {
            this.shortcut.setTag(getAdapterPosition());
            this.shortcut.setOnClickListener(TaskAdapter.this);
            this.title.setText(item.getTitle());
            this.cover.setImageURI(Uri.parse(ConnectionManager.getCoverUrl(item.getID())));
            TextView textView = this.subtitle;
            if (textView != null) {
                textView.setText(item.getSongDescription());
            }
            this.time.setText(item.getTimeString());
            if (this.album != null) {
                if (this.itemView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    this.cover.setVisibility(View.VISIBLE);
                    this.album.setVisibility(View.VISIBLE);
                    this.artist.setVisibility(View.VISIBLE);
                    this.album.setText(item.getAlbum());
                    this.artist.setText(item.getArtist());
                } else {
                    this.cover.setVisibility(View.GONE);
                    this.album.setVisibility(View.GONE);
                    this.artist.setVisibility(View.GONE);
                }
            }
            this.progressbar.setProgress(TaskAdapter.this.taskManager.getProgress(item.getUniqueKey()));
        }
    }
}