package com.whisperyao.dsplayer.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.cast.MediaTrack;
//import com.google.firebase.analytics.FirebaseAnalytics;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.CoverUriLoader;
import com.whisperyao.dsplayer.ServiceOperator;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.UDCEvent;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.adapters.SongListAdapter;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.CoverUtil;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.widget.RatingBar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;


public class ContainerSongFragment extends ContentFragment implements AbsAdapter.Callback {
    private static final String LOG = "ContainerSongFragment";
    protected boolean isForceLoadContent;
    private SimpleDraweeView mAlbumBackView;
    private AlbumInfo mAlbumInfo;
    private CoverDetailHelper mCoverDetailHelper;
    private ArrayList<SongItem> mItems;
    private int page;
    private SongListAdapter songListAdapter;

    @Override
    public boolean canSetView() {
        return false;
    }

    @Override
    public void onTrackOrderChanged(int pos) {
    }

    @Override
    public void toggleView() { }

    @Override
    public void onItemSelected(int count) {
        this.mSelectedItemSum = count;
        this.mSelectModeAdapter.notifyDataSetChanged();
        updateModeMenu();
    }

    @Override
    public void scrollToTop() {

    }

    private static class AlbumInfo {
        private String mDisplayArtist;
        private String mTitle;

        AlbumInfo(String albumArtist, String displayArtist, String title, String id) {
            this.mDisplayArtist = displayArtist;
            this.mTitle = title;
        }

        public String getTitle() {
            return this.mTitle;
        }

        public String getDisplayArtist() {
            return this.mDisplayArtist;
        }
    }

    private class CoverDetailHelper {
        private SimpleDraweeView mAlbumBackView;
        private TextView mAlbumTextLand;
        private TextView mAlbumTextPort;
        private View mArtistRowLand;
        private TextView mArtistTextLand;
        private TextView mArtistTextPort;
        private Context mContext;
        private SimpleDraweeView mCoverImageLand;
        private SimpleDraweeView mCoverImagePort;
        private View mDetailLayoutLand;
        private View mDetailLayoutPort;
        private boolean mEnabled = true;
        private boolean mIsPortrait = true;
        private TextView mMusicTextLand;
        private TextView mMusicTextPort;
        private RatingBar mRatingBarLand;
        private RatingBar mRatingBarPort;
        private View mRatingRowLand;
        private TextView mTitleView;

        public CoverDetailHelper(Context context) {
            this.mContext = context;
        }

        public void setupViews(View detailLayoutPort, View detailLayoutLand, TextView titleView, SimpleDraweeView mvView) {
            this.mDetailLayoutPort = detailLayoutPort;
            ContainerSongFragment containerSongFragment = ContainerSongFragment.this;
            containerSongFragment.mLoadingView = containerSongFragment.mContentView.findViewById(R.id.content_progress);
            ((LinearLayout) ContainerSongFragment.this.mContentView.findViewById(R.id.content_header)).removeView(this.mDetailLayoutPort);
            this.mCoverImagePort = this.mDetailLayoutPort.findViewById(R.id.content_cover);
            this.mAlbumTextPort = this.mDetailLayoutPort.findViewById(R.id.content_album);
            this.mArtistTextPort = this.mDetailLayoutPort.findViewById(R.id.content_artist);
            this.mMusicTextPort = this.mDetailLayoutPort.findViewById(R.id.content_music);
            this.mRatingBarPort = this.mDetailLayoutPort.findViewById(R.id.content_rating);
            this.mDetailLayoutLand = detailLayoutLand;
            this.mCoverImageLand = detailLayoutLand.findViewById(R.id.content_cover);
            this.mAlbumTextLand = this.mDetailLayoutLand.findViewById(R.id.content_album);
            this.mArtistTextLand = this.mDetailLayoutLand.findViewById(R.id.content_artist);
            this.mMusicTextLand = this.mDetailLayoutLand.findViewById(R.id.content_music);
            this.mRatingBarLand = this.mDetailLayoutLand.findViewById(R.id.content_rating);
            this.mArtistRowLand = this.mDetailLayoutLand.findViewById(R.id.content_artist_row);
            this.mRatingRowLand = this.mDetailLayoutLand.findViewById(R.id.content_rating_row);
            this.mTitleView = titleView;
            this.mAlbumBackView = mvView;
        }

        public void setEnabled(boolean enabled) {
            this.mEnabled = enabled;
        }

        public void setIsPortrait(boolean isPortrait) {
            this.mIsPortrait = isPortrait;
            if (ContainerSongFragment.this.songListAdapter.getRealItemCount() > 0) {
                updateVisibility();
            }
        }

        private void updateVisibility() {
            if (this.mEnabled && this.mIsPortrait) {
                ContainerSongFragment.this.songListAdapter.setHeader(this.mDetailLayoutPort);
            } else {
                ContainerSongFragment.this.songListAdapter.disableHeader();
            }
            View view = this.mDetailLayoutPort;
            if (view != null) {
                view.setVisibility((this.mEnabled && this.mIsPortrait) ? View.VISIBLE : View.GONE);
            }
            View view2 = this.mDetailLayoutLand;
            if (view2 != null) {
                view2.setVisibility((!this.mEnabled || this.mIsPortrait) ? View.GONE : View.VISIBLE);
            }
            this.mTitleView.setVisibility(StateManager.getInstance().isMobileLayout() && !this.mIsPortrait ? View.VISIBLE : View.GONE);
        }

        private void setCoverResId(int resId) {
            this.mCoverImagePort.setImageResource(resId);
            this.mCoverImageLand.setImageResource(resId);
        }

        private void updateAlbumDetails(String title, AlbumInfo albumInfo) {
            String title2 = albumInfo.getTitle();
            String displayArtist = albumInfo.getDisplayArtist();
            this.mAlbumTextPort.setText(title2);
            this.mAlbumTextLand.setText(title2);
            if (!TextUtils.isEmpty(displayArtist)) {
                this.mArtistTextPort.setVisibility(View.VISIBLE);
                this.mArtistTextLand.setVisibility(View.VISIBLE);
                View view = this.mArtistRowLand;
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                this.mArtistTextPort.setText(displayArtist);
                this.mArtistTextLand.setText(displayArtist);
            } else {
                this.mArtistTextPort.setVisibility(View.GONE);
                this.mArtistTextLand.setVisibility(View.GONE);
                View view2 = this.mArtistRowLand;
                if (view2 != null) {
                    view2.setVisibility(View.GONE);
                }
            }
            this.mTitleView.setText(title);
        }

        private void updateRating(float rating) {
            if (rating >= 0.0f) {
                this.mRatingBarPort.setVisibility(View.VISIBLE);
                this.mRatingBarLand.setVisibility(View.VISIBLE);
                this.mRatingRowLand.setVisibility(View.VISIBLE);
                this.mRatingBarPort.setRating(rating);
                this.mRatingBarLand.setRating(rating);
                return;
            }
            this.mRatingBarPort.setVisibility(View.GONE);
            this.mRatingBarLand.setVisibility(View.GONE);
            this.mRatingRowLand.setVisibility(View.GONE);
            this.mRatingBarPort.setRating(0.0f);
            this.mRatingBarLand.setRating(0.0f);
        }

        private void updateSongCount(int songCount) {
            String string = this.mContext.getString(R.string.songs_count, Integer.valueOf(songCount));
            this.mMusicTextPort.setText(string);
            this.mMusicTextLand.setText(string);
        }

        private void updateAlbumCover(Bundle argument, Common.ContainerType type) {
            CoverUriLoader coverUriLoaderContainerType = new CoverUriLoader().with(this.mCoverImagePort).placeHolder(type).containerType(type);
            CoverUriLoader coverUriLoaderFailureImage = new CoverUriLoader().with(this.mAlbumBackView).containerType(type).failureImage(type);
            CoverUriLoader coverUriLoaderContainerType2 = new CoverUriLoader().with(this.mCoverImageLand).placeHolder(type).containerType(type);
            if (ContainerSongFragment.this.isOnline) {
                coverUriLoaderContainerType.load(argument);
                coverUriLoaderFailureImage.blur(15).load(argument);
                coverUriLoaderContainerType2.load(argument);
            } else if (ContainerSongFragment.this.songListAdapter.getData() != null) {
                for (SongItem next : ContainerSongFragment.this.songListAdapter.getData()) {
                    File coverFileFromSong = new CoverUtil(App.getContext()).getCoverFileFromSong(next);
                    if (coverFileFromSong != null && coverFileFromSong.exists()) {
                        coverUriLoaderContainerType.legacy(Uri.fromFile(coverFileFromSong)).load(next);
                        coverUriLoaderFailureImage.blur(15).legacy(Uri.fromFile(coverFileFromSong)).load(next);
                        coverUriLoaderContainerType2.legacy(Uri.fromFile(coverFileFromSong)).load(next);
                        return;
                    }
                }
            }
        }
    }

    public ContainerSongFragment() {
        this.isForceLoadContent = false;
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.mAlbumInfo = new AlbumInfo("", "", "", "");
        this.mAlbumBackView = null;
    }

    public ContainerSongFragment(ContentFragment.ContentCallback callback, boolean load) {
        super(callback);
        this.isForceLoadContent = false;
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.mAlbumInfo = new AlbumInfo("", "", "", "");
        this.mAlbumBackView = null;
        this.blLoadContent = load;
    }

    public ContainerSongFragment(ContentFragment.ContentCallback callback, boolean load, boolean doRefresh) {
        super(callback);
        this.isForceLoadContent = false;
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.mAlbumInfo = new AlbumInfo("", "", "", "");
        this.mAlbumBackView = null;
        this.blLoadContent = load;
        this.blDoRefresh = doRefresh;
    }

    @Override
    public boolean isPlayable() {
        ArrayList<SongItem> arrayList = this.mItems;
        return arrayList != null && arrayList.size() > 0;
    }

    @Override
    public boolean canMultiEdit() {
        ArrayList<SongItem> arrayList = this.mItems;
        return arrayList != null && arrayList.size() > 0;
    }

    @Override
    public void markAllItem(boolean mark) {
        if (mark) {
            this.songListAdapter.checkAll();
        } else {
            this.songListAdapter.unCheckAll();
        }
        updateModeMenu();
        if (this.mSelectModeAdapter != null) {
            this.mSelectModeAdapter.notifyDataSetChanged();
        }
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
        this.mType = Common.ContainerType.valueOf(this.mArgument.getString(Common.CONTAINER_TYPE));
        if (this.mArgument.containsKey("title")) {
            this.mTitle = this.mArgument.getString("title");
        }
        if (this.mArgument.containsKey(MediaTrack.ROLE_SUBTITLE)) {
            this.mSubtitle = this.mArgument.getString(MediaTrack.ROLE_SUBTITLE);
        }
        if (this.mArgument.containsKey("ui_info")) {
            Bundle bundle = this.mArgument.getBundle("ui_info");
            this.mAlbumInfo = new AlbumInfo(bundle.getString("album_artist"), bundle.getString(PinManager.DISPLAY_ARTIST), bundle.getString("title"), bundle.getString("id"));
        }
        if (this.mArgument.containsKey(SongItem.SQL_RATING)) {
            this.mRating = this.mArgument.getFloat(SongItem.SQL_RATING);
        }
        SongListAdapter songListAdapter = new SongListAdapter(this);
        this.songListAdapter = songListAdapter;
        songListAdapter.setType(this.mType);
        this.songListAdapter.setIsListMode(true);
        this.mCoverDetailHelper = new CoverDetailHelper(getActivity());
        this.mCoverDetailHelper.setEnabled((this.mType.equals(Common.ContainerType.RANDOM100_MODE) || this.mType.equals(Common.ContainerType.RATING_MODE) || this.mType.equals(Common.ContainerType.SEARCH_SONG_MODE)) ? false : true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SynoLog.d(LOG, "onCreateView");
        this.mContentView = inflater.inflate(R.layout.content_fragment, null);
        return this.mContentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        if (this.blLoadContent) {
            onPageSelected();
        }
        this.mContainerClickCallback.onUpdateTitle();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!StateManager.getInstance().isMobileLayout()) {
            this.songListAdapter.notifyDataSetChanged();
        }
        this.songListAdapter.setSpan(getSpan());
        this.mCoverDetailHelper.setIsPortrait(isProtract());
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPageSelected() {
        if (!this.isInitialized() || this.isForceLoadContent) {
            loadContent(this.blDoRefresh);
            this.setInitialized(true);
            this.blDoRefresh = false;
            this.isForceLoadContent = false;
        }
    }

    private void updateModeMenu() {
        if (this.mMode == null || this.mMode.getMenu() == null) {
            return;
        }
        ArrayList<SongItem> selectedItems = getSelectedItems();
        boolean z = this.songListAdapter.getSelectedItems().size() > 0;
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

    @Override
    public int getScrollToPosition() {
        return this.songListAdapter.getScrollToPosition();
    }

    private void setupViews() {
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
        this.mTitleView = this.mContentView.findViewById(R.id.content_title);
        this.mAlbumBackView = this.mContentView.findViewById(R.id.container_detail_album_back);
        View viewFindViewById = this.mContentView.findViewById(R.id.content_detail_layout_land);
        this.mCoverDetailHelper.setupViews(this.mContentView.findViewById(R.id.content_detail_layout_port), viewFindViewById, this.mTitleView, this.mAlbumBackView);
        this.mCoverDetailHelper.setIsPortrait(isProtract());
        adjustHeader();
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), getSpan(), RecyclerView.VERTICAL, false));
        this.mRecyclerView.setAdapter(this.songListAdapter);
        this.songListAdapter.addEmptyView(this.mEmptyView, false);
        this.songListAdapter.setFastScroller(this.mFastScroller);
        this.songListAdapter.setOnItemClickListener(new AbsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int i) {
                if (R.id.shortcut == view.getId() || R.id.SongItemShortCut == view.getId()) {
                    getQuickAction(view, (SongItem) obj).show();
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
        updateAlbumDetails();
        onConfigurationChanged(this.mActivity.getResources().getConfiguration());
        this.mRecyclerView.setVisibility(View.VISIBLE);
    }



    private void adjustHeader() {
        ((LinearLayout) this.mContentView.findViewById(R.id.content_main)).removeView((LinearLayout) this.mContentView.findViewById(R.id.content_header));
    }

    private void updateAlbumDetails() {
        this.mCoverDetailHelper.updateAlbumDetails(this.mTitle, this.mAlbumInfo);
        this.mCoverDetailHelper.updateRating(this.mRating);
        setBigCoverView();
    }

    private void setBigCoverView() {
        boolean zEquals = getResources().getString(R.string.all_songs).equals(this.mTitle);
        int i = zEquals ? R.drawable.thumbnail_all : R.drawable.thumbnail_album;
        this.mCoverDetailHelper.setCoverResId(i);
        if (!zEquals) {
            this.mCoverDetailHelper.updateAlbumCover(this.mArgument, this.mType);
        }
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
                ContainerSongFragment.this.getActivity().setProgressBarIndeterminateVisibility(true);
                if (refresh) {
                    ContainerSongFragment.this.page = 0;
                    ContainerSongFragment.this.mItems.clear();
                }
                ContainerSongFragment.this.page++;
                if (1 == ContainerSongFragment.this.page) {
                    ContainerSongFragment.this.setRefreshing(true);
                }
            }

            @Override
            public void onWorking() {
                if (ContainerSongFragment.this.mType.equals(Common.ContainerType.SEARCH_SONG_MODE)) {
                    try {
                        List<SongItem> listDoSearch = ConnectionManager.doSearch(
                                Common.SearchCategory.TITLE,
                                ContainerSongFragment.this.mArgument.getString("key")
                        );
                        this.retItems = listDoSearch;
                        ContainerSongFragment.this.total = listDoSearch.size();
                        this.connectionInfo = Common.ConnectionInfo.SUCCESS;
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        CacheManager.ItemSet<SongItem> itemSetDoEnumContainerSongsForContainer = ContainerSongFragment.this.cacheMgr
                                .doEnumContainerSongsForContainer(
                                        ContainerSongFragment.this.isOnline,
                                        ContainerSongFragment.this.mType,
                                        ContainerSongFragment.this.mArgument,
                                        ContainerSongFragment.this.page, refresh
                                );
                        this.retItems = itemSetDoEnumContainerSongsForContainer.getItemList();
                        ContainerSongFragment.this.total = itemSetDoEnumContainerSongsForContainer.getTotal();
                        if (Common.ContainerType.RANDOM100_MODE.equals(ContainerSongFragment.this.mType) && ContainerSongFragment.this.total > 100) {
                            ContainerSongFragment.this.total = 100;
                        }
                        this.connectionInfo = Common.ConnectionInfo.SUCCESS;
                    } catch (WebAPIErrorException e3) {
                        this.setException(e3);
                    }
                }
                if (ContainerSongFragment.this.mRating != -1.0f) {
                    ContainerSongFragment.this.mRating = Utilities.getAvgRating(this.retItems);
                }
            }

            @Override
            public void postWork() {
                if (1 == ContainerSongFragment.this.page) {
                    ContainerSongFragment.this.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                ContainerSongFragment.this.setRefreshing(false);
                if (this.getException() != null) {
                    ContainerSongFragment.this.handleError(this.getException());
                    if (ContainerSongFragment.this.songListAdapter.getRealItemCount() == 0) {
                        ContainerSongFragment.this.songListAdapter.disableHeader();
                        ContainerSongFragment.this.songListAdapter.setData(null);
                        return;
                    }
                    return;
                }
                ContainerSongFragment.this.getActivity().setProgressBarIndeterminateVisibility(false);
                if (Common.ConnectionInfo.SUCCESS.equals(this.connectionInfo)) {
                    ContainerSongFragment.this.mItems.addAll(this.retItems);
                    if (ContainerSongFragment.this.mItems.isEmpty()) {
                        ContainerSongFragment.this.songListAdapter.disableHeader();
                        ContainerSongFragment.this.setNoDataView();
                    } else {
                        ContainerSongFragment.this.mCoverDetailHelper.updateVisibility();
                    }
                    ContainerSongFragment.this.songListAdapter.setData(ContainerSongFragment.this.mItems);
                    ContainerSongFragment.this.mCoverDetailHelper.updateAlbumCover(ContainerSongFragment.this.mArgument, ContainerSongFragment.this.mType);
                    ContainerSongFragment.this.mContainerClickCallback.onUpdateTitle();
                    ContainerSongFragment.this.mContainerClickCallback.onFinishLoading(ContainerSongFragment.this.mType, ContainerSongFragment.this.total);
                    if (ContainerSongFragment.this.canLoadMore() && !this.retItems.isEmpty()) {
                        ContainerSongFragment.this.loadContent(false);
                    }
                }
                ContainerSongFragment.this.mCoverDetailHelper.updateSongCount(ContainerSongFragment.this.mItems.size());
                if (ContainerSongFragment.this.mRating != -1.0f) {
                    ContainerSongFragment.this.mCoverDetailHelper.updateRating(ContainerSongFragment.this.mRating);
                }
            }
        };
        this.loadContentWork.startWork();
    }

    private PopupMenu getQuickAction(final View anchor, final SongItem songItem) {
        PopupMenu popupMenu = new PopupMenu(getContext(), anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            ArrayList<SongItem> arrayList = new ArrayList<>();
            arrayList.add(songItem);
            Bundle bundle = new Bundle();
            int menuItemId = menuItem.getItemId();
            if (menuItemId == R.id.ItemAction_ADDTO_PLAYLIST) {
                listPlaylistOption(songItem);
                bundle.putString(UDCEvent.KEY_MANAGE, "add_to_playlist");
            } else if (menuItemId == R.id.ItemAction_ADD_ITEM) {
                enqueueAction(Common.PlaybackAction.ADD_ONLY, 0, arrayList);
                bundle.putString(UDCEvent.KEY_PLAYBACK, "add_to_queue");
            } else if (menuItemId == R.id.ItemAction_ADD_NEXT) {
                enqueueAction(Common.PlaybackAction.ADD_NEXT, 0, arrayList);
                bundle.putString(UDCEvent.KEY_PLAYBACK, "add_next_to current");
            } else if (menuItemId == R.id.ItemAction_DELETE) {
                if (!isOnline) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.delete)
                            .setMessage(R.string.remove_select)
                            .setPositiveButton(R.string.yes,
                                    (dialogInterface, i) ->
                                            deleteSelected(songItem)).setNegativeButton(R.string.no, null).show();
                }
            } else if (menuItemId == R.id.ItemAction_DOWNLOAD) {
                if (isOnline) {
                    if (songItem.isFile() && Utilities.shouldManualDownload(songItem) && !ServiceOperator.isDownloading(songItem)) {
                        downloadRemote(songItem);
                        bundle.putString(UDCEvent.KEY_MANAGE, "download");
                    }
                } else {
                    bundle.putString(UDCEvent.KEY_MANAGE, "download");
                }
            } else if (menuItemId == R.id.ItemAction_PLAY) {
                enqueueAction(Common.PlaybackAction.PLAY_NOW, 0, arrayList);
                bundle.putString(UDCEvent.KEY_PLAYBACK, "android_play");
            } else if (menuItemId == R.id.ItemAction_RATING) {
                rateSongs(arrayList);
                bundle.putString(UDCEvent.KEY_MANAGE, "rate");
            } else if (menuItemId == R.id.ItemAction_SHARING) {
                shareSongs(arrayList);
                // FirebaseAnalytics.Event.SHARE
                bundle.putString(UDCEvent.KEY_MANAGE, "share");
            }
            // this.firebaseAnalyticsUtil.logEvent(UDCEvent.EVENT__OPERATION_SINGLE_SONG, bundle);
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
            if (Utilities.shouldManualDownload(songItem) && !ServiceOperator.isDownloading(songItem)) {
                popupMenu.getMenu().findItem(R.id.ItemAction_DOWNLOAD).setVisible(true);
            }
        } else {
            popupMenu.getMenu().findItem(R.id.ItemAction_DELETE).setVisible(true);
        }
        return popupMenu;
    }


    @Override
    protected void setToNeedRefresh() {
        this.isForceLoadContent = true;
        this.blDoRefresh = true;
    }
}