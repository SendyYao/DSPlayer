package com.whisperyao.dsplayer.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
//import com.google.firebase.analytics.FirebaseAnalytics;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.PlaylistEditor;
import com.whisperyao.dsplayer.ServiceOperator;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.UDCEvent;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.adapters.PlaylistSongListAdapter;
import com.whisperyao.dsplayer.adapters.SimpleItemTouchHelperCallback;
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.provider.AudioDatabaseUtils;
import com.whisperyao.dsplayer.publicsharing.fragment.ShowSingleSongShareLinksFragment;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;


public class PlaylistSongFragment extends ContentFragment implements ShowSingleSongShareLinksFragment.Callbacks, AbsAdapter.Callback {
    private static final String LOG = "PlaylistSongFragment";

    @Inject
    AudioDatabaseUtils mAudioDatabaseUtils;
    private ArrayList<Integer> mHoldIndex;
    ArrayList<Integer> mHoldSongIndex;
    private ArrayList<SongItem> mItems;
    private PlaylistItem mPlaylistItem;
    ActionMode mReorderMode;
    private ArrayList<SongItem> mSelectedItems;
    private int page;
    private ThreadWork playlistEditWork;
    private PlaylistSongListAdapter songListAdapter;

    @Override
    public boolean canSetView() {
        return false;
    }

    @Override
    public void onPageSelected() {
    }

    @Override
    public void onTrackOrderChanged(int pos) {
    }

    @Override
    public void toggleView() {
    }

    public PlaylistSongFragment() {
        this.mItems = new ArrayList<>();
        this.mSelectedItems = new ArrayList<>();
        this.page = 0;
        this.mHoldIndex = new ArrayList<>();
        this.mReorderMode = null;
        this.mHoldSongIndex = new ArrayList<>();
    }

    public PlaylistSongFragment(ContentFragment.ContentCallback callback) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.mSelectedItems = new ArrayList<>();
        this.page = 0;
        this.mHoldIndex = new ArrayList<>();
        this.mReorderMode = null;
        this.mHoldSongIndex = new ArrayList<>();
    }

    public PlaylistSongFragment(ContentFragment.ContentCallback callback, boolean doRefresh) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.mSelectedItems = new ArrayList<>();
        this.page = 0;
        this.mHoldIndex = new ArrayList<>();
        this.mReorderMode = null;
        this.mHoldSongIndex = new ArrayList<>();
        this.blDoRefresh = doRefresh;
    }

    @Override
    public boolean isReorderable() {
        ArrayList<SongItem> arrayList = this.mItems;
        return arrayList != null && arrayList.size() > 1;
    }

    @Override
    public boolean isPlayable() {
        ArrayList<SongItem> arrayList = this.mItems;
        return arrayList != null && !arrayList.isEmpty();
    }

    @Override
    public boolean canMultiEdit() {
        ArrayList<SongItem> arrayList = this.mItems;
        return arrayList != null && !arrayList.isEmpty();
    }

    @Override
    public void markAllItem(boolean mark) {
        if (mark) {
            this.songListAdapter.checkAll();
        } else {
            this.songListAdapter.unCheckAll();
        }
        updateModeMenu();
    }

    @Override
    public void allItemPlayAction(Common.ItemAction action) {
        if (isPlayable()) {
            if (Common.ItemAction.PLAY.equals(action)) {
                enqueueAction(Common.PlaybackAction.PLAY_NOW, 0, this.mItems);
            } else if (Common.ItemAction.ADD_ITEM.equals(action)) {
                enqueueAction(Common.PlaybackAction.ADD_ONLY, 0, this.mItems);
            }
        }
    }

    @Override
    public void setEditMode(boolean edit) {
        if (this.blEditMode == edit) {
            return;
        }
        this.blEditMode = edit;
        this.songListAdapter.setIsCheckMode(this.blEditMode);
        this.mHoldIndex = this.songListAdapter.getCheckedSongIndex();
        this.mSelectedItems = this.songListAdapter.getSelectedItems();
        markAllItem(false);
        if (this.blEditMode) {
            if (this.mActionModeCallback != null) {
                this.mMode = this.mActionModeCallback.enterActionMode(new ContentFragment.MyActionMode());
            }
            updateModeMenu();
        } else {
            if (this.mMode == null || this.mActionModeCallback == null) {
                return;
            }
            this.mActionModeCallback.leaveActionMode();
        }
    }

    @Override
    public boolean isEditMode() {
        return this.blEditMode;
    }

    @Override
    public ArrayList<SongItem> getSelectedItems() {
        return this.songListAdapter.getSelectedItems();
    }

    @Override
    protected void onScrollToBottom(AbsListView view) {
        if ((this.loadContentWork == null || !this.loadContentWork.isWorking()) && canLoadMore()) {
            loadContent(false);
        }
    }

    @Override
    protected boolean canLoadMore() {
        return this.mItems.size() < this.total;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SynoLog.d(LOG, "onCreate");
        this.isOnline = this.mArgument.getBoolean(PinManager.MODE);
        this.page = 0;
        this.mItems = new ArrayList<>();
        this.mType = Common.ContainerType.valueOf(this.mArgument.getString(Common.CONTAINER_TYPE));
        if (this.mArgument.containsKey("title")) {
            this.mTitle = this.mArgument.getString("title");
        }
        if (this.mArgument.containsKey(PinManager.EXTRA_PLAYLIST)) {
            this.mPlaylistItem = PlaylistItem.Companion.fromBundle(this.mArgument.getBundle(PinManager.EXTRA_PLAYLIST));
        }
        PlaylistSongListAdapter playlistSongListAdapter = new PlaylistSongListAdapter(this);
        this.songListAdapter = playlistSongListAdapter;
        playlistSongListAdapter.setIsListMode(true);
    }

    @Override
    public void onDetach() {
        SynoLog.d(LOG, this.mType.name() + " onDetach");
        ThreadWork threadWork = this.playlistEditWork;
        if (threadWork != null && threadWork.isWorking()) {
            this.playlistEditWork.endThread();
        }
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SynoLog.d(LOG, "onCreateView");
        Utils.registerReceiver(getActivity().getApplicationContext(), this.getMPlaylistChangedListener(), new IntentFilter(Common.ACTION_PLAYLIST_CHANGED), false);
        this.mContentView = inflater.inflate(R.layout.playlistsong_fragment, (ViewGroup) null);
        setupViews();
        if (!this.isInitialized()) {
            loadContent(this.blDoRefresh);
            this.setInitialized(true);
            this.blDoRefresh = false;
        } else if (this.mItems.isEmpty()) {
            this.mEmptyView.setVisibility(View.VISIBLE);
            setNoDataView();
        }
        this.mContainerClickCallback.onUpdateTitle();
        return this.mContentView;
    }

    @Override
    public void onDestroyView() {
        SynoLog.d(LOG, "onDestroyView");
        super.onDestroyView();
        getActivity().getApplicationContext().unregisterReceiver(this.getMPlaylistChangedListener());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!StateManager.getInstance().isMobileLayout()) {
            this.songListAdapter.notifyDataSetChanged();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SynoLog.d(LOG, "onPrepareOptionsMenu");
        MenuItem menuItemFindItem = menu.findItem(R.id.menu_reorder);
        if (menuItemFindItem.isVisible()) {
            PlaylistItem playlistItem = this.mPlaylistItem;
            if (((playlistItem != null && !playlistItem.isPredefined()) && this.mType.isNormalPlaylistType() && ((this.mType.isSharedPlaylistType() && Common.editSharedPlaylist()) || (this.mType.isPersonalPlaylistType() && Common.editPersonalPlaylist())))){
                menuItemFindItem.setVisible(true);
            }
            if (this.mPlaylistItem.isLocal()) {
                menuItemFindItem.setVisible(true);
            }
        }
        if (!ConnectionManager.canSupportPin() || !this.isOnline) {
            menu.findItem(R.id.menu_pin).setVisible(false);
            menu.findItem(R.id.menu_unpin).setVisible(false);
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("playlist", this.mPlaylistItem.getID());
        if (PinManager.Companion.getInstance().alreadyPin("playlist", PinManager.Companion.getPinCriteria(this.mType, bundle))) {
            menu.findItem(R.id.menu_pin).setVisible(false);
        } else {
            menu.findItem(R.id.menu_unpin).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SynoLog.d(LOG, "onOptionsItemSelected : " + ((Object) item.getTitle()));
        if (R.id.menu_reorder == item.getItemId()) {
            setReorderMode(true);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(int count) {
        this.mSelectedItemSum = count;
        updateModeMenu();
    }

    private void updateModeMenu() {
        if (this.mMode == null || this.mMode.getMenu() == null) {
            return;
        }
        ArrayList<SongItem> selectedItems = getSelectedItems();
        boolean z = !selectedItems.isEmpty();
        if (this.mSelectModeAdapter != null) {
            this.mSelectModeAdapter.notifyDataSetChanged();
        }
        MenuItem menuItemFindItem = this.mMode.getMenu().findItem(R.id.editmenu_group_play);
        if (menuItemFindItem != null) {
            menuItemFindItem.setEnabled(z);
        }
        Iterator<SongItem> it = selectedItems.iterator();
        boolean z2 = true;
        while (it.hasNext()) {
            if (!ConnectionManager.canEditRating(this.isOnline, it.next())) {
                z2 = false;
            }
        }
        MenuItem menuItemFindItem2 = this.mMode.getMenu().findItem(R.id.editmenu_rating);
        if (menuItemFindItem2 != null) {
            menuItemFindItem2.setVisible(ConnectionManager.canEditRating(this.isOnline));
            menuItemFindItem2.setEnabled(z && z2);
        }
        Iterator<SongItem> it2 = selectedItems.iterator();
        boolean z3 = true;
        while (it2.hasNext()) {
            if (it2.next().isRadio()) {
                z3 = false;
            }
        }
        MenuItem menuItemFindItem3 = this.mMode.getMenu().findItem(R.id.editmenu_download);
        if (menuItemFindItem3 != null) {
            menuItemFindItem3.setEnabled(z && z3);
        }
        Iterator<SongItem> it3 = selectedItems.iterator();
        boolean z4 = true;
        while (it3.hasNext()) {
            if (!ConnectionManager.canShareSong(this.isOnline, it3.next())) {
                z4 = false;
            }
        }
        MenuItem menuItemFindItem4 = this.mMode.getMenu().findItem(R.id.editmenu_share);
        if (menuItemFindItem4 != null) {
            menuItemFindItem4.setVisible(ConnectionManager.canSharePlaylist(this.isOnline));
            menuItemFindItem4.setEnabled(z && z4);
        }
        MenuItem menuItemFindItem5 = this.mMode.getMenu().findItem(R.id.editmenu_delete);
        if (menuItemFindItem5 != null) {
            menuItemFindItem5.setEnabled(z);
        }
        MenuItem menuItemFindItem6 = this.mMode.getMenu().findItem(R.id.editmenu_add_to_playlist);
        if (menuItemFindItem6 != null) {
            menuItemFindItem6.setEnabled(z);
        }
    }

    private void setupViews() {
        this.mLoadingView = this.mContentView.findViewById(R.id.content_progress);
        this.mRefresh = this.mContentView.findViewById(R.id.refresh);
        this.mRefresh.setOnRefreshListener(() -> {
            if (loadContentWork == null || !loadContentWork.isWorking()) {
                doRefresh();
            }
        });
        this.mRefresh.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        this.mEmptyView = this.mContentView.findViewById(R.id.content_empty);
        this.mEmptyTextView = this.mContentView.findViewById(R.id.tv_no_data);
        this.mEmptyImageView = this.mContentView.findViewById(R.id.icon_no_data);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        this.songListAdapter.addEmptyView(this.mEmptyView, false);
        this.songListAdapter.setOnItemClickListener(new AbsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int i) {
                if (R.id.SongItemShortCut == view.getId()) {
                    getQuickAction(view, (SongItem) obj, i).show();
                    return;
                }
                ArrayList<SongItem> arrayList = new ArrayList<>(songListAdapter.getData());
                if (Common.TapSongAction.ADD.equals(AudioPreference.getTapSongPref())) {
                    enqueueAction(Common.PlaybackAction.BY_SITUACTION, i, arrayList, false);
                } else {
                    enqueueAction(Common.PlaybackAction.PLAY_NOW, i, arrayList, false);
                }
            }
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(this.songListAdapter));
        this.songListAdapter.setTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(this.mRecyclerView);
        this.mRecyclerView.setAdapter(this.songListAdapter);
        this.songListAdapter.setFastScroller(this.mFastScroller);
        this.mTitleView = (TextView) this.mContentView.findViewById(R.id.content_title);
        if (!StateManager.getInstance().isMobileLayout() || TextUtils.isEmpty(this.mTitle)) {
            return;
        }
        this.mTitleView.setVisibility(View.VISIBLE);
        this.mTitleView.setText(this.mTitle);
    }

    @Override
    public void loadContent(final boolean refresh) {
        SynoLog.d(LOG, "loadContent(" + refresh + "), type = " + this.mType.name());
        if (this.loadContentWork != null && this.loadContentWork.isWorking()) {
            this.loadContentWork.endThread();
        }
        this.loadContentWork = new ThreadWork() {
            Common.ConnectionInfo connectionInfo = Common.ConnectionInfo.ERROR_NETWORK;
            List<SongItem> retItems = new LinkedList<>();

            @Override
            public void preWork() {
                PlaylistSongFragment.this.getActivity().setProgressBarIndeterminateVisibility(true);
                if (refresh) {
                    PlaylistSongFragment.this.page = 0;
                    PlaylistSongFragment.this.mItems.clear();
                }
                PlaylistSongFragment.this.page++;
                if (1 == PlaylistSongFragment.this.page) {
                    PlaylistSongFragment.this.setRefreshing(true);
                }
            }

            @Override
            public void onWorking() {
                try {
                    if (PlaylistSongFragment.this.mPlaylistItem.isLocal()) {
                        List<SongItem> listDoEnumLocalPlaylistSongs = PlaylistSongFragment.this.mAudioDatabaseUtils
                                .doEnumLocalPlaylistSongs(
                                        PlaylistSongFragment.this.mPlaylistItem.getDsId(),
                                        PlaylistSongFragment.this.mPlaylistItem.getID(),
                                        PlaylistSongFragment.this.mPlaylistItem.getTitle()
                                );
                        this.retItems = listDoEnumLocalPlaylistSongs;
                        PlaylistSongFragment.this.total = listDoEnumLocalPlaylistSongs.size();
                    } else {
                        CacheManager.ItemSet<SongItem> itemSetDoEnumPlaylistSongsForPlaylist =
                                PlaylistSongFragment.this.cacheMgr
                                        .doEnumPlaylistSongsForPlaylist(
                                                PlaylistSongFragment.this.isOnline,
                                                PlaylistSongFragment.this.mPlaylistItem,
                                                PlaylistSongFragment.this.page, refresh
                                        );
                        this.retItems = itemSetDoEnumPlaylistSongsForPlaylist.getItemList();
                        PlaylistSongFragment.this.total = itemSetDoEnumPlaylistSongsForPlaylist.getTotal();
                    }
                    this.connectionInfo = Common.ConnectionInfo.SUCCESS;
                } catch (WebAPIErrorException e) {
                    this.setException(e);
                }
            }

            @Override
            public void postWork() {
                if (1 == PlaylistSongFragment.this.page) {
                    PlaylistSongFragment.this.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                PlaylistSongFragment.this.getActivity().setProgressBarIndeterminateVisibility(false);
                if (this.getException() != null) {
                    PlaylistSongFragment.this.handleError(this.getException());
                    if (PlaylistSongFragment.this.songListAdapter.getRealItemCount() == 0) {
                        PlaylistSongFragment.this.songListAdapter.setData(null);
                        return;
                    }
                    return;
                }
                if (Common.ConnectionInfo.SUCCESS.equals(this.connectionInfo)) {
                    PlaylistSongFragment.this.mItems.addAll(this.retItems);
                    if (PlaylistSongFragment.this.mItems.isEmpty()) {
                        PlaylistSongFragment.this.setNoDataView();
                    }
                    PlaylistSongFragment.this.songListAdapter.setData(PlaylistSongFragment.this.mItems);
                    PlaylistSongFragment.this.mContainerClickCallback.onUpdateTitle();
                    if (!PlaylistSongFragment.this.canLoadMore() || this.retItems.size() <= 0) {
                        return;
                    }
                    PlaylistSongFragment.this.loadContent(false);
                }
            }
        };
        this.loadContentWork.startWork();
    }

    private void setReorderMode(boolean set) {
        if (set) {
            this.songListAdapter.setIsDragMode(true);
            if (this.mActionModeCallback != null) {
                this.mReorderMode = this.mActionModeCallback.enterActionMode(new ReorderMode());
            }
        } else {
            this.songListAdapter.setIsDragMode(false);
            if (this.mActionModeCallback != null) {
                this.mActionModeCallback.leaveActionMode();
            }
            ActionMode actionMode = this.mReorderMode;
            if (actionMode != null) {
                actionMode.finish();
                this.mReorderMode = null;
            }
        }
        this.songListAdapter.notifyDataSetChanged();
    }

    @Override
    public void scrollToTop() {

    }

    protected final class ReorderMode implements ActionMode.Callback {
        private boolean mApplyOrder = false;

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        protected ReorderMode() {
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            PlaylistSongFragment.this.mActivity.getMenuInflater().inflate(R.menu.playingq_edit_menu_drag, menu);
            PlaylistSongFragment.this.mRefresh.setEnabled(false);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.editmenu_ok) {
                this.mApplyOrder = true;
                PlaylistSongFragment playlistSongFragment = PlaylistSongFragment.this;
                playlistSongFragment.mHoldSongIndex = playlistSongFragment.songListAdapter.getHoldSongIndex();
                int size = PlaylistSongFragment.this.mHoldSongIndex.size() - 1;
                int i = 0;
//                while (i == PlaylistSongFragment.this.mHoldSongIndex.get(i) && (i = i + 1) < size) { }
//                while (size == PlaylistSongFragment.this.mHoldSongIndex.get(size) && i < size - 1) { }
                if (i < size) {
                    int i2 = (size - i) + 1;
                    int[] iArr = new int[i2];
                    for (int i3 = 0; i3 < i2; i3++) {
                        iArr[i3] = PlaylistSongFragment.this.mHoldSongIndex.get(i3 + i);
                    }
                    PlaylistSongFragment.this.updatePlaylist(i, i2, iArr);
                }
                SynoLog.d(PlaylistSongFragment.LOG, "start = " + i + ", end = " + size + ", mHoldSongIndex = " + PlaylistSongFragment.this.mHoldSongIndex.toString());
            }
            PlaylistSongFragment.this.mReorderMode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            PlaylistSongFragment.this.mReorderMode = null;
            if (this.mApplyOrder) {
                PlaylistSongFragment.this.songListAdapter.applyOrder();
            } else {
                PlaylistSongFragment.this.songListAdapter.undoOrder();
            }
            PlaylistSongFragment.this.setReorderMode(false);
            PlaylistSongFragment.this.mRefresh.setEnabled(true);
        }
    }

    private PopupMenu getQuickAction(final View anchor, final SongItem songItem, final int songPos) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            SynoLog.d(LOG, "onActionItemClick : " + menuItem.getTitle());

            ArrayList<SongItem> arrayList = new ArrayList<>();
            arrayList.add(songItem);

            Bundle bundle = new Bundle();
            int itemId = menuItem.getItemId();

            if (itemId == R.id.ItemAction_ADDTO_PLAYLIST) {
                listPlaylistOption(songItem);
                bundle.putString(UDCEvent.KEY_MANAGE, "add_to_playlist");

            } else if (itemId == R.id.ItemAction_ADD_ITEM) {
                enqueueAction(Common.PlaybackAction.ADD_ONLY, 0, arrayList);

                bundle.putString(UDCEvent.KEY_PLAYBACK, "add_to_queue");

            } else if (itemId == R.id.ItemAction_ADD_NEXT) {
                enqueueAction(Common.PlaybackAction.ADD_NEXT, 0, arrayList);

                bundle.putString(UDCEvent.KEY_PLAYBACK, "add_next_to current");

            } else if (itemId == R.id.ItemAction_DELETE) {
                new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.remove_select)
                        .setPositiveButton(
                                R.string.yes,
                                (dialogInterface, i) ->
                                        deletePlaylistSong(i)
                        )
                        .setNegativeButton(R.string.no, null)
                        .show();

            } else if (itemId == R.id.ItemAction_DOWNLOAD) {
                if (songItem.isFile()
                        && Utilities.shouldManualDownload(songItem)
                        && !ServiceOperator.isDownloading(songItem)) {

                    // downloadRemote(songItem);
                    bundle.putString(UDCEvent.KEY_MANAGE, "download");
                }

            } else if (itemId == R.id.ItemAction_PLAY) {
                enqueueAction(Common.PlaybackAction.PLAY_NOW, 0, arrayList);

                bundle.putString(UDCEvent.KEY_PLAYBACK, "android_play");

            } else if (itemId == R.id.ItemAction_RATING) {
                // rateSongs(arrayList);
                bundle.putString(UDCEvent.KEY_MANAGE, "rate");

            } else if (itemId == R.id.ItemAction_SHARING) {
                shareSongs(arrayList);

                // FirebaseAnalytics.Event.SHARE
                bundle.putString(UDCEvent.KEY_MANAGE, "share");
            }

//            firebaseAnalyticsUtil.logEvent(UDCEvent.EVENT__OPERATION_SINGLE_SONG, bundle);

            return false;
        });
        popupMenu.inflate(R.menu.file_song_menu);
        popupMenu.getMenu().findItem(R.id.ItemAction_PLAY).setVisible(true);
        popupMenu.getMenu().findItem(R.id.ItemAction_ADD_ITEM).setVisible(true);
        if (ConnectionManager.canSupportAddToNext()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_ADD_NEXT).setVisible(true);
        }
        if (this.isOnline && Common.createPersonalPlaylist()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_ADDTO_PLAYLIST).setVisible(true);
        }
        if (ConnectionManager.canEditRating(this.isOnline, songItem)) {
            popupMenu.getMenu().findItem(R.id.ItemAction_RATING).setVisible(true);
        }
        if (ConnectionManager.canShareSong(this.isOnline, songItem)) {
            popupMenu.getMenu().findItem(R.id.ItemAction_SHARING).setVisible(true);
        }
        if (this.isOnline) {
            if (songItem.isFile() && Utilities.shouldManualDownload(songItem) && !ServiceOperator.isDownloading(songItem)) {
                popupMenu.getMenu().findItem(R.id.ItemAction_DOWNLOAD).setVisible(true);
            }
            if (this.mType.isNormalPlaylistType() && ((this.mType.isPersonalPlaylistType() && Common.editPersonalPlaylist()) || this.mType.isSharedPlaylistType() && Common.editSharedPlaylist())) {
                popupMenu.getMenu().findItem(R.id.ItemAction_DELETE).setVisible(true);
            }
        }
        if (this.mPlaylistItem.isLocal()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_DELETE).setVisible(true);
        }
        return popupMenu;
    }

//    @Override
//    protected void deleteSelected(final List<SongItem> songs) {
//        if (this.mHoldIndex.isEmpty()) {
//            return;
//        }
//        ThreadWork threadWork = this.playlistEditWork;
//        if (threadWork != null && threadWork.isWorking()) {
//            this.playlistEditWork.endThread();
//        }
//        ThreadWork threadWork2 = new ThreadWork() {
//
//            Common.ConnectionInfo info;
//            final ProgressDialog myDialog;
//            boolean success = false;
//
//            {
//                this.myDialog = new ProgressDialog(PlaylistSongFragment.this.mActivity);
//            }
//
//            @Override
//            public void preWork() {
//                this.myDialog.setMessage(PlaylistSongFragment.this.getResources().getString(R.string.processing));
//                this.myDialog.setCancelable(false);
//                this.myDialog.show();
//            }
//
//            @Override
//            public void onWorking() {
//                try {
//                    if (PlaylistSongFragment.this.mPlaylistItem.isLocal()) {
//                        PlaylistSongFragment.this.mAudioDatabaseUtils.deleteLocalPlaylistSongRelation(songs);
//                        Common.ConnectionInfo connectionInfo = Common.ConnectionInfo.SUCCESS;
//                        this.info = connectionInfo;
//                        connectionInfo.setResultVo(BaseVo.getSuccessBaseVo());
//                    } else if (PlaylistSongFragment.this.mPlaylistItem.isMostPlayed() || PlaylistSongFragment.this.mPlaylistItem.isRecentPlayed()) {
//                        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
//                        if (databaseAccesser != null) {
//                            databaseAccesser.resetHitSong(PlaylistSongFragment.this.mSelectedItems);
//                            databaseAccesser.close();
//                            Common.ConnectionInfo connectionInfo2 = Common.ConnectionInfo.SUCCESS;
//                            this.info = connectionInfo2;
//                            connectionInfo2.setResultVo(BaseVo.getSuccessBaseVo());
//                        }
//                    } else {
//                        int iIntValue = PlaylistSongFragment.this.mHoldIndex.get(0);
//                        int iIntValue2 = PlaylistSongFragment.this.mHoldIndex.get(PlaylistSongFragment.this.mHoldIndex.size() - 1).intValue();
//                        int i = (iIntValue2 - iIntValue) + 1;
//                        ArrayList arrayList = new ArrayList();
//                        for (int i2 = iIntValue; i2 < iIntValue2; i2++) {
//                            if (!PlaylistSongFragment.this.mHoldIndex.contains(i2)) {
//                                arrayList.add(PlaylistSongFragment.this.songListAdapter.getData().get(i2));
//                            }
//                        }
//                        this.info = PlaylistEditor.doUpdatePlaylist(
//                                PlaylistSongFragment.this.mPlaylistItem.getID(),
//                                iIntValue,
//                                i,
//                                Utilities.createIdList(arrayList),
//                                tag -> PlaylistSongFragment.this.playlistEditWork.setTag(tag));
//                    }
//                    this.success = true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onComplete() {
//                this.myDialog.dismiss();
//                if (this.success && this.info.getResultVo() != null && this.info.getResultVo().getSuccess()) {
//                    PlaylistSongFragment.this.doRefresh();
//                } else {
//                    Toast.makeText(PlaylistSongFragment.this.mActivity, R.string.operation_failed, 0).show();
//                }
//            }
//        };
//        this.playlistEditWork = threadWork2;
//        threadWork2.startWork();
//    }

    private void updatePlaylist(final int start, final int limit, final int[] replaceIdx) {
        ThreadWork threadWork = this.playlistEditWork;
        if (threadWork != null && threadWork.isWorking()) {
            this.playlistEditWork.endThread();
        }
        ThreadWork threadWork2 = new ThreadWork() {

            Common.ConnectionInfo f51info;
            final ProgressDialog myDialog;
            boolean success = false;

            {
                this.myDialog = new ProgressDialog(PlaylistSongFragment.this.mActivity);
            }

            @Override
            public void preWork() {
                this.myDialog.setMessage(PlaylistSongFragment.this.getResources().getString(R.string.processing));
                this.myDialog.setCancelable(false);
                this.myDialog.show();
            }

            @Override
            public void onWorking() {
                try {
                    int i = 0;
                    if (PlaylistSongFragment.this.mPlaylistItem.isLocal()) {
                        int size = PlaylistSongFragment.this.mHoldSongIndex.size();
                        ArrayList<SongItem> arrayList = new ArrayList<>();
                        for (int i2 = 0; i2 < start; i2++) {
                            arrayList.add(PlaylistSongFragment.this.mItems.get(i2));
                        }
                        while (i < replaceIdx.length) {
                            arrayList.add(PlaylistSongFragment.this.mItems.get(replaceIdx[i]));
                            i++;
                        }
                        for (int i3 = start + limit; i3 < size; i3++) {
                            arrayList.add(PlaylistSongFragment.this.mItems.get(i3));
                        }
                        PlaylistSongFragment.this.mAudioDatabaseUtils.deleteLocalPlaylistSongRelation(arrayList);
                        PlaylistSongFragment.this.mAudioDatabaseUtils.saveSongsInDownloadPlaylist(PlaylistSongFragment.this.mPlaylistItem, arrayList);
                        Common.ConnectionInfo connectionInfo = Common.ConnectionInfo.SUCCESS;
                        this.f51info = connectionInfo;
                        connectionInfo.setResultVo(BaseVo.getSuccessBaseVo());
                    } else {
                        ArrayList arrayList2 = new ArrayList<>();
                        while (i < replaceIdx.length) {
                            arrayList2.add(PlaylistSongFragment.this.mItems.get(replaceIdx[i]));
                            i++;
                        }
                        this.f51info = PlaylistEditor.doUpdatePlaylist(
                                PlaylistSongFragment.this.mPlaylistItem.getID(),
                                start,
                                limit,
                                Utilities.createIdList(arrayList2),
                                tag -> PlaylistSongFragment.this.playlistEditWork.setTag(tag));
                    }
                    this.success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onComplete() {
                this.myDialog.dismiss();
                if (this.success && this.f51info.getResultVo() != null && this.f51info.getResultVo().getSuccess()) {
                    PlaylistSongFragment.this.doRefresh();
                } else {
                    Toast.makeText(PlaylistSongFragment.this.mActivity, this.f51info.getStringId(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        this.playlistEditWork = threadWork2;
        threadWork2.startWork();
    }

    private void deletePlaylistSong(final int pos) {
        ThreadWork threadWork = this.playlistEditWork;
        if (threadWork != null && threadWork.isWorking()) {
            this.playlistEditWork.endThread();
        }
        ThreadWork threadWork2 = new ThreadWork() {

            Common.ConnectionInfo info;
            final ProgressDialog myDialog;
            boolean success = false;

            {
                this.myDialog = new ProgressDialog(PlaylistSongFragment.this.mActivity);
            }

            @Override
            public void preWork() {
                this.myDialog.setMessage(PlaylistSongFragment.this.getResources().getString(R.string.processing));
                this.myDialog.setCancelable(false);
                this.myDialog.show();
            }

            @Override
            public void onWorking() {
                try {
                    if (PlaylistSongFragment.this.mPlaylistItem.isLocal()) {
                        PlaylistSongFragment.this.mAudioDatabaseUtils.deleteLocalPlaylistSongRelation((SongItem) PlaylistSongFragment.this.mItems.get(pos));
                        Common.ConnectionInfo connectionInfo = Common.ConnectionInfo.SUCCESS;
                        this.info = connectionInfo;
                        connectionInfo.setResultVo(BaseVo.getSuccessBaseVo());
                    } else {
                        this.info = PlaylistEditor.doUpdatePlaylist(PlaylistSongFragment.this.mPlaylistItem.getID(), pos, 1, "", (ConnectionManager.GetHttpPost) tag -> PlaylistSongFragment.this.playlistEditWork.setTag(tag));
                    }
                    this.success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onComplete() {
                this.myDialog.dismiss();
                if (this.success && this.info.getResultVo() != null && this.info.getResultVo().getSuccess()) {
                    PlaylistSongFragment.this.doRefresh();
                } else {
                    Toast.makeText(PlaylistSongFragment.this.mActivity, this.info.getStringId(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        this.playlistEditWork = threadWork2;
        threadWork2.startWork();
    }

    @Override
    public void onShared(SongItem song) {
        if (this.mPlaylistItem.isSharedSong()) {
            doRefresh();
        }
    }
}
