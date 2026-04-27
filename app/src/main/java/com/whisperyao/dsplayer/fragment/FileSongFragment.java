package com.whisperyao.dsplayer.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.ServiceOperator;
import com.whisperyao.dsplayer.StateManager;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.UDCEvent;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.adapters.FileSongListAdapter;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.CoverUtil;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import info.hoang8f.android.segmented.SegmentedGroup;

public class FileSongFragment extends ContentFragment implements ContentFragment.ContentCallback, PinManager.Callback, AbsAdapter.Callback {
    private static final String LOG = "FileSongFragment";
    private Bundle contentBundle;
    private ThreadWork enumSongsWork;
    private int foldercount;
    protected boolean isForceLoadContent;
    private boolean isLeft;
    private View listFolderGroupLayout;
    private ContentFragment mContentFrag;
    private FileSongHeaderHelper mFileSongHeaderHelper;
    private FileSongListAdapter mFileSongListAdapter;
    private ArrayList<SongItem> mFolderItems;
    private ArrayList<SongItem> mItems;
    private String mKey;
    private SegmentedGroup mListFolderGroup;
    private ArrayList<SongItem> mSongItems;
    private Common.PrefViewMode mViewMode;
    private int page;
    private int scrollToPos;
    private int selPos;
    private int songcount;

    public FileSongFragment() {
        this.isForceLoadContent = false;
        this.mItems = new ArrayList<>();
        this.mFolderItems = new ArrayList<>();
        this.mKey = "";
        this.page = 0;
        this.songcount = 0;
        this.foldercount = 0;
        this.isLeft = true;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
    }

    public FileSongFragment(ContentCallback callback, boolean load) {
        super(callback);
        this.isForceLoadContent = false;
        this.mItems = new ArrayList<>();
        this.mFolderItems = new ArrayList<>();
        this.mSongItems = new ArrayList<>();
        this.mKey = "";
        this.page = 0;
        this.songcount = 0;
        this.foldercount = 0;
        this.isLeft = true;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
        this.blLoadContent = load;
    }

    public FileSongFragment(ContentCallback callback, boolean load, boolean doRefresh) {
        super(callback);
        this.isForceLoadContent = false;
        this.mItems = new ArrayList<>();
        this.mFolderItems = new ArrayList<>();
        this.mSongItems = new ArrayList<>();
        this.mKey = "";
        this.page = 0;
        this.songcount = 0;
        this.foldercount = 0;
        this.isLeft = true;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
        this.blLoadContent = load;
        this.blDoRefresh = doRefresh;
    }

    @Override
    public void onItemSelected(int count) {
        mSelectedItemSum = count;
        if (this.mSelectModeAdapter != null) {
            this.mSelectModeAdapter.notifyDataSetChanged();
        }
        updateModeMenu();
    }

    private void updateModeMenu() {
        if (this.mMode == null || this.mMode.getMenu() == null) {
            return;
        }
        ArrayList<SongItem> selectedItems = getSelectedItems();
        boolean z = !selectedItems.isEmpty();
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
            MenuItem menuItemFindItem2 = this.mMode.getMenu().findItem(R.id.editmenu_rating);
            if (menuItemFindItem2 != null) {
                menuItemFindItem2.setVisible(ConnectionManager.canEditRating(this.isOnline));
                menuItemFindItem2.setEnabled(z && z2);
            }
            MenuItem menuItemFindItem3 = this.mMode.getMenu().findItem(R.id.editmenu_download);
            if (menuItemFindItem3 != null) {
                menuItemFindItem3.setEnabled(z);
            }
            Iterator<SongItem> it2 = selectedItems.iterator();
            boolean z3 = true;
            while (it2.hasNext()) {
                if (!ConnectionManager.canShareSong(this.isOnline, it2.next())) {
                    z3 = false;
                }
            }
            MenuItem menuItemFindItem4 = this.mMode.getMenu().findItem(R.id.editmenu_share);
            if (menuItemFindItem4 != null) {
                menuItemFindItem4.setVisible(ConnectionManager.canSharePlaylist(this.isOnline));
                menuItemFindItem4.setEnabled(z && z3);
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
    }

    @Override
    public void onTrackOrderChanged(int playingPos) {
    }

    @Override
    public void toggleView() {
        ContentFragment contentFragment;
        boolean z = this.isLeft;
        if (z && (contentFragment = this.mContentFrag) != null) {
            contentFragment.toggleView();
        } else if ((!z || StateManager.getInstance().isMobileLayout()) && this.mFileSongHeaderHelper.isFolderGroup()) {
            this.mViewMode = AudioPreference.getViewMode();
            showView(true);
        }

    }

    public void showView(boolean show) {
        if (show) {
            this.mFileSongListAdapter.setIsListMode(!Common.PrefViewMode.THUMBNAIL.equals(this.mViewMode));
            this.mRecyclerView.setVisibility(View.VISIBLE);
            return;
        }
        this.mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onPageSelected() {
    }

    @Override
    public void allItemPlayAction(Common.@Nullable ItemAction action) {
        ContentFragment contentFragment;
        if (this.isLeft && (contentFragment = this.mContentFrag) != null) {
            contentFragment.allItemPlayAction(action);
            return;
        }
        if (isPlayable()) {
            ArrayList<SongItem> arrayList = new ArrayList<>();
            for (SongItem next : this.mItems) {
                if (!next.getType().isDirectory()) {
                    arrayList.add(next);
                }
            }
            if (Common.ItemAction.PLAY.equals(action)) {
                enumSongs(R.id.ItemAction_PLAY, 0, arrayList);
            } else if (Common.ItemAction.ADD_ITEM.equals(action)) {
                enumSongs(R.id.ItemAction_ADD_ITEM, 0, arrayList);
            }
        }
    }

    private void enumSongs(final int itemAction, final int position, final ArrayList<SongItem> items) {
        enumSongs(itemAction, position, items, true);
    }
    private void enumSongs(final int itemAction, final int position, final ArrayList<SongItem> items, final boolean isFromMenu) {
        final ProgressDialog progressDialog = new ProgressDialog(mActivity);
        progressDialog.setMessage(getResources().getString(R.string.processing));
        progressDialog.setCancelable(false);

        enumSongsWork = new ThreadWork() {

            private final ArrayList<SongItem> songList = new ArrayList<>();

            @Override
            public void preWork() {
                progressDialog.show();
            }

            @Override
            public void onWorking() {
                for (SongItem songItem : items) {
                    if (songItem.isFile()) {
                        songList.add(songItem);
                        continue;
                    }

                    if (itemAction == R.id.ItemAction_DOWNLOAD
                            && songItem.isFile()
                            && Utilities.shouldManualDownload(songItem)) {
                        continue;
                    }

                    songList.addAll(cacheMgr.doEnumFolderSongsForFileSongList(isOnline, songItem.getID(), true, -1, true).getItemList());
                }
            }
            @Override
            public void onComplete() {
                progressDialog.dismiss();

                Bundle bundle = new Bundle();

                if (itemAction == R.id.ItemAction_ADDTO_PLAYLIST) {
                    listPlaylistOption(songList);
                    bundle.putString(UDCEvent.KEY_MANAGE, "add_to_playlist");

                } else if (itemAction == R.id.ItemAction_ADD_ITEM) {
                    enqueueAction(Common.PlaybackAction.ADD_ONLY, position, songList, isFromMenu);
                    bundle.putString(UDCEvent.KEY_PLAYBACK, "add_to_queue");

                } else if (itemAction == R.id.ItemAction_ADD_NEXT) {
                    enqueueAction(Common.PlaybackAction.ADD_NEXT, position, songList, isFromMenu);
                    bundle.putString(UDCEvent.KEY_PLAYBACK, "add_next_to_current");

                } else if (itemAction == R.id.ItemAction_BY_SITUATION) {
                    enqueueAction(Common.PlaybackAction.BY_SITUACTION, position, songList, isFromMenu);

                } else if (itemAction == R.id.ItemAction_DELETE) {
                    deleteSelected(songList);

                } else if (itemAction == R.id.ItemAction_DOWNLOAD) {
//                    downloadRemote(songList);
                    bundle.putString(UDCEvent.KEY_MANAGE, "download");

                } else if (itemAction == R.id.ItemAction_PLAY) {
                    enqueueAction(Common.PlaybackAction.PLAY_NOW, position, songList, isFromMenu);
                    bundle.putString(UDCEvent.KEY_PLAYBACK, "android_play");

                } else if (itemAction == R.id.ItemAction_RATING) {
//                    rateSongs(songList);
                    bundle.putString(UDCEvent.KEY_MANAGE, "rate");

                } else if (itemAction == R.id.ItemAction_SHARING) {
                    shareSongs(songList);
//                    bundle.putString(UDCEvent.KEY_MANAGE, FirebaseAnalytics.Event.SHARE);
                }

//                firebaseAnalyticsUtil.logEvent(UDCEvent.EVENT__OPERATION_SINGLE_SONG, bundle);
            }
        };

        enumSongsWork.startWork();
    }

    public Bundle getEnumSongsBundle(Item item) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(PinManager.MODE, this.isOnline);
        bundle.putString("id", item.getID());
        return bundle;
    }

    private PopupMenu getQuickAction(final View anchor, final SongItem item) {
        PopupMenu popupMenu = getPopupMenu(anchor, item);
        // ConnectionManager.canSupportPin()
        if (this.isOnline && !item.isFile()) {
            this.mArgument.putString("folder", item.getID());
//            PinManager.Companion.getInstance().alreadyPin("folder", PinManager.getPinCriteria(this.mType, this.mArgument))
            if (true) {
                popupMenu.getMenu().findItem(R.id.ItemAction_UNPIN).setVisible(true);
            } else {
                popupMenu.getMenu().findItem(R.id.ItemAction_PIN).setVisible(true);
            }
        }
        popupMenu.getMenu().findItem(R.id.ItemAction_PLAY).setVisible(true);
        popupMenu.getMenu().findItem(R.id.ItemAction_ADD_ITEM).setVisible(true);
        if (ConnectionManager.canSupportAddToNext()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_ADD_NEXT).setVisible(true);
        }
        if (this.isOnline && Common.createPersonalPlaylist()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_ADDTO_PLAYLIST).setVisible(true);
        }
        if (ConnectionManager.canEditRating(this.isOnline, item)) {
            popupMenu.getMenu().findItem(R.id.ItemAction_RATING).setVisible(true);
        }
        if (ConnectionManager.canShareSong(this.isOnline, item)) {
            popupMenu.getMenu().findItem(R.id.ItemAction_SHARING).setVisible(true);
        }
        if (this.isOnline) {
            if (Utilities.shouldManualDownload(item) && !ServiceOperator.isDownloading(item)) {
                popupMenu.getMenu().findItem(R.id.ItemAction_DOWNLOAD).setVisible(true);
            }
        } else {
            popupMenu.getMenu().findItem(R.id.ItemAction_DELETE).setVisible(true);
        }
        return popupMenu;
    }

    @NonNull
    private PopupMenu getPopupMenu(View anchor, SongItem songItem) {
        PopupMenu popupMenu = new PopupMenu(getContext(), anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            SynoLog.d(LOG, "onActionItemClick : " + menuItem.getTitle());
            final ArrayList<SongItem> arrayList = new ArrayList<>();
            arrayList.add(songItem);
            int itemId = menuItem.getItemId();
            if (itemId == R.id.ItemAction_DELETE) {
                new AlertDialog.Builder(mActivity).setTitle(R.string.delete).setMessage(R.string.remove_select).setPositiveButton(R.string.yes, (dialogInterface, i) -> enumSongs(menuItem.getItemId(), 0, arrayList)).setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null).show();
            } else if (itemId == R.id.ItemAction_PIN) {
//                    String quickActionTypeParamName = PinManager.getQuickActionTypeParamName(this.mType);
                mArgument.putString("folder", songItem.getID());
//                    PinManager.Companion.getInstance().pin(quickActionTypeParamName, PinManager.getPinCriteria(this.mType, this.mArgument), songItem.getTitle());
            } else if (itemId == R.id.ItemAction_UNPIN) {
//                    String quickActionTypeParamName2 = PinManager.getQuickActionTypeParamName(this.mType);
                mArgument.putString("folder", songItem.getID());
//                    PinManager.Companion.getInstance().unpin(PinManager.Companion.getInstance().getPinId(quickActionTypeParamName2, PinManager.getPinCriteria(this.mType, this.mArgument)));
            } else {
                enumSongs(menuItem.getItemId(), 0, arrayList);
            }
            return false;

        });
        popupMenu.inflate(R.menu.file_song_menu);
        return popupMenu;
    }

    @Override
    protected boolean canLoadMore() {
        return false;
    }

    @Override
    public boolean canMultiEdit() {
        return false;
    }

    @Override
    public boolean canSetView() {
        return false;
    }

    @Override
    public @Nullable ArrayList<@NotNull SongItem> getSelectedItems() {
        return this.mFileSongListAdapter.getSelectedItems();
    }

    @Override
    public boolean isEditMode() {
        return false;
    }

    @Override
    public boolean isPlayable() {
        ContentFragment contentFragment;
        if (!this.isLeft || (contentFragment = this.mContentFrag) == null) {
            return StateManager.getInstance().isMobileLayout() || !this.isLeft;
        }
        return contentFragment.isPlayable();
    }

    @Override
    public void setEditMode(boolean edit) {
        ContentFragment contentFragment;
        this.mFileSongHeaderHelper.showFolderGroup(edit);
        if (this.isLeft && (contentFragment = this.mContentFrag) != null) {
            if (contentFragment != null) {
                contentFragment.setEditMode(edit);
            }
        } else {
            if (this.blEditMode == edit) {
                return;
            }
            this.blEditMode = edit;
            this.mFileSongListAdapter.setIsCheckMode(this.blEditMode);
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

    }

    @Override
    public void loadContent(boolean refresh) {
        SynoLog.d(LOG, this.isOnline + " loadContent(" + refresh + "), type = " + this.mType.name());
        if (this.loadContentWork != null && this.loadContentWork.isWorking()) {
            this.loadContentWork.endThread();
        }
        this.loadContentWork = new ThreadWork() {
            Common.ConnectionInfo connectionInfo = Common.ConnectionInfo.ERROR_NETWORK;
            List<SongItem> retItems = new LinkedList();
            final boolean refresh = blDoRefresh;

            @Override
            public void preWork() {
                FileSongFragment.this.getActivity().setProgressBarIndeterminateVisibility(true);
                if (this.refresh) {
                    FileSongFragment.this.page = 0;
                    FileSongFragment.this.songcount = 0;
                    FileSongFragment.this.foldercount = 0;
                    FileSongFragment.this.mItems.clear();
                    FileSongFragment.this.mSongItems.clear();
                    FileSongFragment.this.mFolderItems.clear();
                }
                FileSongFragment.this.page++;
                if (1 == FileSongFragment.this.page) {
                    FileSongFragment.this.setRefreshing(true);
                }
            }

            @Override
            public void onWorking() {
                try {
                    CacheManager.ItemSet<SongItem> itemSetDoEnumFolderSongsForSongFileList = FileSongFragment.this.cacheMgr
                            .doEnumFolderSongsForSongFileList(FileSongFragment.this.isOnline, FileSongFragment.this.mKey, false, FileSongFragment.this.page, this.refresh);
                    this.retItems = itemSetDoEnumFolderSongsForSongFileList.getItemList();
                    FileSongFragment.this.total = itemSetDoEnumFolderSongsForSongFileList.getTotal();
                    this.connectionInfo = Common.ConnectionInfo.SUCCESS;
                } catch (WebAPIErrorException e) {

                }
            }

            @Override
            public void onComplete() {
                if (FileSongFragment.this.isAdded()) {
                    if (this.getException() != null) {
                        FileSongFragment.this.handleError(this.getException());
                        if (FileSongFragment.this.mFileSongListAdapter.getRealItemCount() == 0) {
                            FileSongFragment.this.mFileSongListAdapter.setData(null);
                            return;
                        }
                        return;
                    }
                    for (SongItem songItem : this.retItems) {
                        if (songItem.isFile()) {
                            FileSongFragment.this.songcount++;
                            FileSongFragment.this.mSongItems.add(songItem);
                        } else {
                            FileSongFragment.this.foldercount++;
                            FileSongFragment.this.mFolderItems.add(songItem);
                        }
                    }
                    if (1 == FileSongFragment.this.page) {
                        FileSongFragment.this.setRefreshing(false);
                        FileSongFragment.this.showView(true);
                    }
                    FileSongFragment.this.mFileSongHeaderHelper.checkSongCount();
                    FileSongFragment.this.getActivity().setProgressBarIndeterminateVisibility(false);
                    if (this.connectionInfo == Common.ConnectionInfo.SUCCESS) {
                        FileSongFragment.this.mItems.addAll(this.retItems);
                        if (FileSongFragment.this.mItems.isEmpty()) {
                            FileSongFragment.this.setNoDataView();
                            if (FileSongFragment.this.mContentFrag != null) {
                                FileSongFragment.this.getChildFragmentManager().beginTransaction().remove(FileSongFragment.this.mContentFrag).commit();
                            }
                        }
                        if (FileSongFragment.this.mListFolderGroup == null) {
                            if (!FileSongFragment.this.mFolderItems.isEmpty()) {
                                FileSongFragment.this.mFileSongListAdapter.setData(FileSongFragment.this.mFolderItems);
                            } else {
                                FileSongFragment.this.mFileSongListAdapter.setData(FileSongFragment.this.mSongItems);
                            }
                        } else if (FileSongFragment.this.mListFolderGroup.getCheckedRadioButtonId() == R.id.foldergroup_folder) {
                            FileSongFragment.this.mFileSongListAdapter.setData(FileSongFragment.this.mFolderItems);
                            FileSongFragment.this.mRecyclerView.post(() -> FileSongFragment.this.mRecyclerView.scrollToPosition(FileSongFragment.this.scrollToPos));
                        } else {
                            FileSongFragment.this.mFileSongListAdapter.setData(FileSongFragment.this.mSongItems);
                        }
                        FileSongFragment.this.mFileSongHeaderHelper.updateAlbumCover();
                        FileSongFragment.this.mContainerClickCallback.onUpdateTitle();
                        if (FileSongFragment.this.songcount > 0) {
                            FileSongFragment.this.mFileSongHeaderHelper.updateSongCount(FileSongFragment.this.songcount);
                        }
                        if (!FileSongFragment.this.canLoadMore() || this.retItems.isEmpty()) {
                            return;
                        }
                        FileSongFragment.this.loadContent(false);
                    }
                }

            }
        };
        this.loadContentWork.startWork();
    }

    @Override
    public void markAllItem(boolean mark) {
        if (mark) {
            this.mFileSongListAdapter.checkAll();
        } else {
            this.mFileSongListAdapter.unCheckAll();
        }
        updateModeMenu();
    }

    @Override
    protected void onScrollToBottom(@Nullable AbsListView view) {
    }

    @Override
    public @Nullable Stack<@NotNull Bundle> getBundleStack() {
        return null;
    }

    @Override
    public void onContainerItemClick(@NotNull Bundle bundle) {
        this.contentBundle.putInt("position", bundle.getInt("position"));
        this.contentBundle.putInt("scroll_to_position", bundle.getInt("scroll_to_position"));
        this.mContainerClickCallback.onContainerItemClick(this.contentBundle);
    }

    @Override
    public void onFinishLoading(Common.@NotNull ContainerType type, int size) {
    }

    @Override
    public void onUpdateTitle() {
        this.mContainerClickCallback.onUpdateTitle();
    }

    @Override
    public void onPinErrorOccur() {
        doRefresh();
    }

    @Override
    public void onPinLoadFinish() {
    }

    @Override
    public void onPinPreLoading() {
    }

    private void setupViews() {
        this.mLoadingView = this.mContentView.findViewById(R.id.content_progress);
        this.mFileSongHeaderHelper.addCustomHeader();
        this.mRefresh = this.mContentView.findViewById(R.id.refresh);
        this.mRefresh.setOnRefreshListener(() -> {
            scrollToPos = 0;
            doRefresh();
        });
        this.mRefresh.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        this.mEmptyView = this.mContentView.findViewById(R.id.content_empty);
        this.mEmptyTextView = this.mContentView.findViewById(R.id.tv_no_data);
        this.mEmptyImageView = this.mContentView.findViewById(R.id.icon_no_data);
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), getSpan(), RecyclerView.VERTICAL, false));
        this.mRecyclerView.setAdapter(this.mFileSongListAdapter);
        this.mFileSongListAdapter.addEmptyView(this.mEmptyView, false);
        this.mFileSongListAdapter.setFastScroller(this.mFastScroller);
        this.mFileSongListAdapter.setOnItemClickListener((View view, SongItem songItem, int i) -> {
            SynoLog.d(LOG, "mFileSongListAdapter.setOnItemClickListener");
            if (R.id.shortcut == view.getId() || R.id.SongItemShortCut == view.getId()) {
                getQuickAction(view, songItem).show();
                return;
            }
            selPos = i;
            scrollToPos = this.mFileSongListAdapter.getScrollToPosition();
            mArgument.putInt("position", selPos);
            mArgument.putInt("scroll_to_position", scrollToPos);
            SynoLog.d(LOG, "songItem.getType = " + songItem.getType());
            if (songItem.getType().isDirectory()) {
                Bundle bundle = new Bundle();
                bundle.putString(Common.CONTAINER_TYPE, mType.name());
                bundle.putBoolean(PinManager.MODE, isOnline);
                bundle.putString("key", songItem.getID());
                bundle.putString("title", songItem.getTitle());
                bundle.putInt("position", i);
                bundle.putInt("scroll_to_position", scrollToPos);
                if (StateManager.getInstance().isMobileLayout() || isLeft) {
                    this.mContainerClickCallback.onContainerItemClick(bundle);
                    return;
                }
                ContentFragment contentFragment = mContentFrag;
                if (contentFragment != null && contentFragment.isEditMode()) {
                    mContentFrag.setEditMode(false);
                }
                contentBundle = (Bundle) bundle.clone();
                bundle.putBoolean("left_pane", false);
                SynoLog.d(LOG, "mFileSongListAdapter.setOnItemClickListener => ContentFragment.Companion.newInstance");
                mContentFrag = ContentFragment.Companion.newInstance(bundle, this, false);
                return;
            }
            ArrayList<SongItem> arrayList = new ArrayList<>();
            for (SongItem next : this.mItems) {
//                SynoLog.d(LOG, "next = " + next);
                if (!next.getType().isDirectory()) {
                    arrayList.add(next);
                }
            }
            SynoLog.d(LOG, String.valueOf(Common.TapSongAction.ADD.equals(AudioPreference.getTapSongPref())));
            if (Common.TapSongAction.ADD.equals(AudioPreference.getTapSongPref())) {
                enumSongs(R.id.ItemAction_BY_SITUATION, i, arrayList, false);
            } else {
                enumSongs(R.id.ItemAction_PLAY, i, arrayList, false);
            }
        });
        this.mTitleView = this.mContentView.findViewById(R.id.content_title);
        if (StateManager.getInstance().isMobileLayout() && !TextUtils.isEmpty(this.mTitle)) {
            this.mTitleView.setVisibility(View.VISIBLE);
            this.mTitleView.setText(this.mTitle);
        }
        this.mViewMode = AudioPreference.getViewMode();
        onConfigurationChanged(this.mActivity.getResources().getConfiguration());
        this.mFileSongHeaderHelper.checkSongCount();
        showView(true);
        updateAlbumCover();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SynoLog.d(LOG, "onCreate");
        this.mType = Common.ContainerType.FOLDER_MODE;
        this.isOnline = this.mArgument.getBoolean(PinManager.MODE);
        this.mKey = this.mArgument.getString("key");
        if (this.mArgument.containsKey("title")) {
            this.mTitle = this.mArgument.getString("title");
        }
        if (this.mArgument.containsKey("left_pane")) {
            this.isLeft = this.mArgument.getBoolean("left_pane");
        } else {
            this.isLeft = this.isOnline;
        }
        if (this.mArgument.containsKey("position")) {
            this.selPos = this.mArgument.getInt("position");
            this.scrollToPos = this.mArgument.getInt("scroll_to_position");
        }
        this.mFileSongListAdapter = new FileSongListAdapter(this);
        this.mFileSongHeaderHelper = new FileSongHeaderHelper(getActivity());
        PinManager.Companion.getInstance().addCallback(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SynoLog.d(LOG, this.isOnline + " onCreateView");
        if (StateManager.getInstance().isMobileLayout() || !this.isLeft) {
            this.mContentView = inflater.inflate(R.layout.content_fragment, null);
        } else {
            this.mContentView = inflater.inflate(R.layout.tablet_content_fragment_recycler, null);
        }
        return this.mContentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        if (this.blLoadContent) {
            onPageSelected();
        }
        loadContent(true);
    }

    @Override
    public void scrollToTop() { }

    private class FileSongHeaderHelper {
        private SimpleDraweeView mAlbumBackView;
        private Context mContext;
        private SimpleDraweeView mCoverImagePort;
        private View mHeader;
        private TextView mMusicTextPort;
        private int mCurrentFolderGroupId = R.id.foldergroup_folder;
        private boolean mProtectFromCheckedChange = false;
        RadioGroup.OnCheckedChangeListener mRadioGroupListener = (group, checkedId) -> {
            if (FileSongHeaderHelper.this.mProtectFromCheckedChange) {
                return;
            }
            FileSongHeaderHelper.this.mProtectFromCheckedChange = true;
            if (checkedId != -1) {
                FileSongHeaderHelper.this.mCurrentFolderGroupId = checkedId;
                FileSongHeaderHelper.this.syncChecked(checkedId);
                FileSongHeaderHelper.this.switchFolderGroup(checkedId);
            }
            FileSongHeaderHelper.this.mProtectFromCheckedChange = false;
        };

        FileSongHeaderHelper(Context context) {
            this.mContext = context;
        }

        boolean isFolderGroup() {
            return this.mCurrentFolderGroupId == R.id.foldergroup_folder;
        }

        public void addCustomHeader() {
            FileSongFragment fileSongFragment = FileSongFragment.this;
            fileSongFragment.listFolderGroupLayout = View.inflate(fileSongFragment.mActivity, R.layout.my_segmented_group, null);
            FileSongFragment fileSongFragment2 = FileSongFragment.this;
            fileSongFragment2.mListFolderGroup = fileSongFragment2.listFolderGroupLayout.findViewById(R.id.foldergroup);
            FileSongFragment.this.mListFolderGroup.setOnCheckedChangeListener(this.mRadioGroupListener);
            View viewInflate = View.inflate(FileSongFragment.this.getContext(), R.layout.container_details_port, null);
            this.mHeader = viewInflate;
            viewInflate.setVisibility(View.VISIBLE);
            View viewFindViewById = this.mHeader.findViewById(R.id.content_rating);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(View.GONE);
            }
            this.mCoverImagePort = this.mHeader.findViewById(R.id.content_cover);
            this.mMusicTextPort = this.mHeader.findViewById(R.id.content_music);
            this.mAlbumBackView = this.mHeader.findViewById(R.id.container_detail_album_back);
            FileSongFragment.this.mFileSongListAdapter.setSegment(FileSongFragment.this.listFolderGroupLayout);
        }

        void syncChecked(int checkedId) {
            if (FileSongFragment.this.mListFolderGroup.getCheckedRadioButtonId() != checkedId) {
                FileSongFragment.this.mListFolderGroup.check(checkedId);
            }
        }

        void checkSongCount() {
            if (FileSongFragment.this.songcount == 0) {
                this.mCurrentFolderGroupId = R.id.foldergroup_folder;
            } else if (FileSongFragment.this.foldercount == 0) {
                this.mCurrentFolderGroupId = R.id.foldergroup_song;
            }
            if (FileSongFragment.this.mListFolderGroup != null) {
                FileSongFragment.this.mListFolderGroup.check(this.mCurrentFolderGroupId);
                showFolderGroup(false);
            }
        }

        void showFolderGroup(boolean edit) {
            if (FileSongFragment.this.foldercount == 0 || FileSongFragment.this.songcount == 0 || edit) {
                FileSongFragment.this.mFileSongListAdapter.disableSegment();
            } else {
                FileSongFragment.this.mFileSongListAdapter.setSegment(FileSongFragment.this.listFolderGroupLayout);
            }
            if (FileSongFragment.this.songcount > 0) {
                updateSongCount(FileSongFragment.this.songcount);
            }
        }

        void switchFolderGroup(int checkedId) {
            if (checkedId == R.id.foldergroup_folder) {
                FileSongFragment.this.mViewMode = AudioPreference.getViewMode();
                FileSongFragment.this.showView(true);
                FileSongFragment.this.mFileSongListAdapter.setData(FileSongFragment.this.mFolderItems);
                FileSongFragment.this.mFileSongListAdapter.disableHeader();
            } else if (checkedId == R.id.foldergroup_song) {
                FileSongFragment.this.mViewMode = Common.PrefViewMode.LIST;
                FileSongFragment.this.showView(true);
                FileSongFragment.this.mFileSongListAdapter.setData(FileSongFragment.this.mSongItems);
                FileSongFragment.this.mFileSongListAdapter.setHeader(this.mHeader);
            }
            if (FileSongFragment.this.getActivity() != null) {
                FileSongFragment.this.getActivity().invalidateOptionsMenu();
            }
        }

        public void updateAlbumCover() {
            File coverFileFromSong;
            if (FileSongFragment.this.mFileSongListAdapter.getData() != null) {
                CoverUtil coverUtil = new CoverUtil(App.getContext());
                Iterator<SongItem> it = FileSongFragment.this.mFileSongListAdapter.getData().iterator();
                SynoLog.d(LOG, "isOnline: " + FileSongFragment.this.isOnline);
                while (it.hasNext()) {
                    SongItem next = it.next();
                    if (FileSongFragment.this.isOnline) {
                        coverFileFromSong = coverUtil.getCoverFileFromSong(next, ConnectionManager.getCoverUrl(next.getID()));
                    } else {
                        coverFileFromSong = coverUtil.getCoverFileFromSong(next);
                    }
                    if (coverFileFromSong != null && coverFileFromSong.exists()) {
                        SynoLog.d(LOG, "coverFileFromSong != null && coverFileFromSong.exists()");
                        new CoverUriLoader().with(this.mCoverImagePort).legacy(Uri.fromFile(coverFileFromSong)).containerType(FileSongFragment.this.mType).failureImage(R.drawable.thumbnail_song).load(next);
                        new CoverUriLoader().with(this.mAlbumBackView).legacy(Uri.fromFile(coverFileFromSong)).containerType(FileSongFragment.this.mType).failureImage(R.drawable.thumbnail_song).blur(15).load(next);
                        return;
                    }
                }
                return;
            }
            new CoverUriLoader().with(this.mCoverImagePort).placeHolder(R.drawable.thumbnail_song);
        }

        public void updateAlbumCover(Bundle argument, Common.ContainerType type) {
            SynoLog.d(LOG, "触发updateAlbumCover(Bundle argument, Common.ContainerType type)");
            SynoLog.d(LOG, "argument: " + argument + " type: " + type);
            new CoverUriLoader().with(this.mCoverImagePort).placeHolder(type).failureImage(type).load(argument);
            new CoverUriLoader().with(this.mAlbumBackView).containerType(type).failureImage(type).blur(15).load(argument);
        }

        public void updateSongCount(int songCount) {
            String string = FileSongFragment.this.getString(R.string.songs_count, Integer.valueOf(songCount));
            TextView textView = this.mMusicTextPort;
            if (textView != null) {
                textView.setText(string);
            }
        }
    }

    private void updateAlbumCover() {
        SynoLog.d(LOG, "updateAlbumCover() this.mFileSongHeaderHelper.updateAlbumCover(bundle, this.mType)");
        if (this.mKey != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(PinManager.MODE, this.isOnline);
            bundle.putString("id", this.mKey);
            this.mFileSongHeaderHelper.updateAlbumCover(bundle, this.mType);
            return;
        }
        this.mFileSongHeaderHelper.updateAlbumCover();
    }

}
