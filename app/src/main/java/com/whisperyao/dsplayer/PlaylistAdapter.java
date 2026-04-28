package com.whisperyao.dsplayer;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.PopupMenu;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlaylistAdapter extends BaseAdapter {
    private static final int ITEM_TYPE_DATA = 1;
    private static final int ITEM_TYPE_HEADER = 0;
    private final static PlaylistCallbacks sDummyCallbacks = (actionId, item) -> { };
    private int list_press;
    private final PlaylistCallbacks mCallbacks;
    private final ArrayList<UiPlaylistItem> mContentList;
    private final Context mContext;
    private final HashMap<String, List<UiPlaylistItem>> mData;
    private final LayoutInflater mInflater;
    private final ArrayList<String> mSections;
    private int selPos;
    private int transparent;

    public interface PlaylistCallbacks {
        void onActionClicked(int actionId, PlaylistItem item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public static class UiPlaylistItem {
        PlaylistItem mItem;
        String mTtitle;
        PlaylistItemType mType;

        private enum PlaylistItemType {
            Header,
            Item
        }

        public UiPlaylistItem(PlaylistItem item) {
            this.mType = PlaylistItemType.Item;
            this.mTtitle = item.getTitle();
            this.mItem = item;
        }

        public UiPlaylistItem(String title) {
            this.mType = PlaylistItemType.Item;
            this.mTtitle = title;
            this.mItem = null;
        }

        public static UiPlaylistItem generateHeader(String title) {
            UiPlaylistItem uiPlaylistItem = new UiPlaylistItem(title);
            uiPlaylistItem.mType = PlaylistItemType.Header;
            return uiPlaylistItem;
        }

        public static UiPlaylistItem generatePlaylistItem(PlaylistItem item) {
            return new UiPlaylistItem(item);
        }

        public String getTitle() {
            return this.mTtitle;
        }

        public boolean isHeader() {
            return this.mType.equals(PlaylistItemType.Header);
        }

        public PlaylistItem getDataItem() {
            return this.mItem;
        }
    }

    public void setSelection(int pos) {
        if (getItem(pos).isHeader()) {
            return;
        }
        this.selPos = pos;
    }

    public PlaylistAdapter(Context context) {
        this(context, sDummyCallbacks);
    }

    public PlaylistAdapter(Context context, PlaylistCallbacks callback) {
        this.selPos = -1;
        this.list_press = 0;
        this.transparent = 0;
        this.mContext = context;
        this.mCallbacks = callback;
        this.mInflater = LayoutInflater.from(App.getContext());
        this.mContentList = new ArrayList<>();
        this.mSections = new ArrayList<>();
        this.mData = new HashMap<>();
        this.list_press = this.mContext.getResources().getColor(R.color.list_press_over_light);
        this.transparent = this.mContext.getResources().getColor(R.color.transparent);
    }

    private void generateContentList() {
        this.mContentList.clear();
        for (int i = 0; i < getGroupCount(); i++) {
            if (getChildrenCount(i) != 0) {
                if (i != 0) {
                    this.mContentList.add(getGroup(i));
                }
                for (int i2 = 0; i2 < getChildrenCount(i); i2++) {
                    this.mContentList.add(getChild(i, i2));
                }
            }
        }
    }

    private UiPlaylistItem getChild(int groupPosition, int childPosition) {
        try {
            return this.mData.get(getGroup(groupPosition).getTitle()).get(childPosition);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getChildrenCount(int groupPosition) {
        try {
            return this.mData.get(getGroup(groupPosition).getTitle()).size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private View getChildView(final UiPlaylistItem item, int position, View convertView) {
        View viewInflate;
        ChildViewHolder childViewHolder;
        if (item == null) {
            return null;
        }
        if (convertView == null) {
            childViewHolder = new ChildViewHolder();
            viewInflate = this.mInflater.inflate(R.layout.playlist_list_item, null);
            childViewHolder.layout = viewInflate.findViewById(R.id.layout_container);
            childViewHolder.title = viewInflate.findViewById(R.id.title);
            childViewHolder.subtitle = viewInflate.findViewById(R.id.subtitle);
            childViewHolder.cover = viewInflate.findViewById(R.id.cover);
            childViewHolder.mark = viewInflate.findViewById(R.id.checkbox);
            childViewHolder.shortcut = viewInflate.findViewById(R.id.shortcut);
            childViewHolder.subtitle.setVisibility(View.GONE);
            childViewHolder.mark.setVisibility(View.GONE);
            viewInflate.setTag(childViewHolder);
        } else {
            viewInflate = convertView;
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }
        if (!StateManager.getInstance().isMobileLayout() && position == this.selPos) {
            childViewHolder.layout.setBackgroundColor(this.list_press);
        } else {
            childViewHolder.layout.setBackgroundColor(this.transparent);
        }
        childViewHolder.title.setText(item.getTitle());
        PlaylistItem dataItem = item.getDataItem();
        childViewHolder.cover.setImageResource(dataItem.getIconResId());
        if ((ConnectionManager.canSupportPin() || !dataItem.isRecentlyAdded()) && dataItem.isWithQuickAction()) {
            childViewHolder.shortcut.setVisibility(View.VISIBLE);
            childViewHolder.shortcut.setOnClickListener(v -> PlaylistAdapter.this.getQuickAction(v, item.getDataItem()).show());
        } else {
            childViewHolder.shortcut.setVisibility(View.GONE);
            childViewHolder.shortcut.setOnClickListener(null);
        }
        return viewInflate;
    }

    private PopupMenu getQuickAction(final View anchor, final PlaylistItem playlistItem) {
        PopupMenu popupMenu = new PopupMenu(this.mContext, anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            PlaylistAdapter.this.mCallbacks.onActionClicked(menuItem.getItemId(), playlistItem);
            return false;
        });
        popupMenu.inflate(R.menu.file_song_menu);
        PinManager.Companion.getInstance().addQuickAction(popupMenu.getMenu().findItem(R.id.ItemAction_PIN), popupMenu.getMenu().findItem(R.id.ItemAction_UNPIN), playlistItem);
        popupMenu.getMenu().findItem(R.id.ItemAction_PLAY).setVisible(!playlistItem.isRecentlyAdded());
        popupMenu.getMenu().findItem(R.id.ItemAction_ADD_ITEM).setVisible(!playlistItem.isRecentlyAdded());
        if (!playlistItem.isRecentlyAdded()) {
            if (ConnectionManager.canSupportAddToNext()) {
                popupMenu.getMenu().findItem(R.id.ItemAction_ADD_NEXT).setVisible(true);
            }
            if (!playlistItem.isPredefined()) {
                if (playlistItem.isLocal()) {
                    popupMenu.getMenu().findItem(R.id.ItemAction_DELETE).setVisible(true);
                } else if ((playlistItem.isPersonal() && Common.editPersonalPlaylist()) || (!playlistItem.isPersonal() && Common.editSharedPlaylist())) {
                    popupMenu.getMenu().findItem(R.id.ItemAction_EDIT).setVisible(true);
                    if (playlistItem.isWithSharing()) {
                        popupMenu.getMenu().findItem(R.id.ItemAction_SHARING).setVisible(true);
                    }
                    popupMenu.getMenu().findItem(R.id.ItemAction_DOWNLOAD).setVisible(true);
                    popupMenu.getMenu().findItem(R.id.ItemAction_DELETE).setVisible(true);
                }
            }
        }
        return popupMenu;
    }

    private class ChildViewHolder {
        public ImageView cover;
        public LinearLayout layout;
        public CheckBox mark;
        public ImageView shortcut;
        public TextView subtitle;
        public TextView title;

        private ChildViewHolder() {
        }
    }

    private UiPlaylistItem getGroup(int groupPosition) {
        return UiPlaylistItem.generateHeader(this.mSections.get(groupPosition));
    }

    private int getGroupCount() {
        return this.mSections.size();
    }

    private View getGroupView(UiPlaylistItem item, View convertView) {
        View viewInflate;
        ChildViewHolder childViewHolder;
        if (convertView == null) {
            childViewHolder = new ChildViewHolder();
            viewInflate = this.mInflater.inflate(R.layout.section_header, null);
            childViewHolder.title = viewInflate.findViewById(R.id.tv_header);
            viewInflate.setTag(childViewHolder);
        } else {
            viewInflate = convertView;
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }
        childViewHolder.title.setText(item.getTitle());
        return viewInflate;
    }

    public void addSection(String section, List<UiPlaylistItem> items) {
        if (!this.mSections.contains(section)) {
            this.mSections.add(section);
        }
        SynoLog.d("PlaylistAdapter", "items: " + items);
        this.mData.put(section, items);
        generateContentList();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isHeader() ? 0 : 1;
    }

    @Override
    public int getCount() {
        return this.mContentList.size();
    }

    @Override
    public UiPlaylistItem getItem(int position) {
        if (position < 0 || position >= this.mContentList.size()) {
            return null;
        }
        return this.mContentList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UiPlaylistItem uiPlaylistItem = this.mContentList.get(position);
        if (getItemViewType(position) == 0) {
            return getGroupView(uiPlaylistItem, convertView);
        }
        return getChildView(uiPlaylistItem, position, convertView);
    }

}
