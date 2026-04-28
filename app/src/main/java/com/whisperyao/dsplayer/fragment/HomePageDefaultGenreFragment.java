package com.whisperyao.dsplayer.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.cast.MediaTrack;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.adapters.DefaultGenreAdapter;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.SynoLog;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;


public class HomePageDefaultGenreFragment extends ContentFragment implements ContentFragment.ContentCallback {
    private static final String LOG = "HomePageDefaultGenreFragment";
    private DefaultGenreAdapter containerGridAdapter;
    private Bundle contentBundle;
    private ThreadWork enumSongsWork;
    private ArrayList<Item> mItems;
    private int page;
    private int scrollToPos;

    @Override
    public void allItemPlayAction(Common.ItemAction action) { }

    @Override
    public boolean canMultiEdit() {
        return false;
    }

    @Override
    public boolean canSetView() {
        return false;
    }

    @Override
    public Stack<Bundle> getBundleStack() {
        return null;
    }

    @Override
    public boolean isEditMode() {
        return false;
    }

    @Override
    public boolean isPlayable() {
        return false;
    }

    @Override
    public void markAllItem(boolean mark) {
    }

    @Override
    public void onFinishLoading(Common.ContainerType type, int size) {
    }

    @Override
    public void setEditMode(final boolean edit) {
    }

    @Override
    public void toggleView() {
    }

    public HomePageDefaultGenreFragment() {
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.scrollToPos = -1;
    }

    public HomePageDefaultGenreFragment(ContentFragment.ContentCallback callback, boolean load) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.scrollToPos = -1;
        this.blLoadContent = load;
    }

    public HomePageDefaultGenreFragment(ContentFragment.ContentCallback callback, boolean load, boolean doRefresh) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.scrollToPos = -1;
        this.blLoadContent = load;
        this.blDoRefresh = doRefresh;
    }

    @Override
    public ArrayList<SongItem> getSelectedItems() {
        return new ArrayList<>();
    }

    private void showView(boolean show) {
        if (show) {
            this.mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            this.mRecyclerView.setVisibility(View.GONE);
        }
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
        this.isOnline = this.mArgument.getBoolean(PinManager.MODE);
        if (this.mArgument.containsKey("scroll_to_position")) {
            this.scrollToPos = this.mArgument.getInt("scroll_to_position");
        } else {
            this.scrollToPos = 0;
        }
        this.mType = Common.ContainerType.valueOf(this.mArgument.getString(Common.CONTAINER_TYPE));
        if (this.mArgument.containsKey("title")) {
            this.mTitle = this.mArgument.getString("title");
        }
        if (this.mArgument.containsKey(MediaTrack.ROLE_SUBTITLE)) {
            this.mSubtitle = this.mArgument.getString(MediaTrack.ROLE_SUBTITLE);
        }
        this.containerGridAdapter = new DefaultGenreAdapter();
    }

    @Override
    public void onDetach() {
        ThreadWork threadWork = this.enumSongsWork;
        if (threadWork != null && threadWork.isWorking()) {
            this.enumSongsWork.endThread();
        }
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    public void onPageSelected() {
        if (this.isInitialized()) {
            return;
        }
        loadContent(this.blDoRefresh);
        this.setInitialized(true);
        this.blDoRefresh = false;
    }

    @Override
    protected void setNoDataView() {
        super.setNoDataView();
    }

    private void onItemClick(int pos, boolean doRefresh) {
        SynoLog.d(LOG, "onItemClick : " + pos);
        Item item = this.containerGridAdapter.getData().get(pos);
        this.scrollToPos = this.containerGridAdapter.getScrollToPosition();
        Bundle enumSongsBundle = getEnumSongsBundle(item);
        enumSongsBundle.putInt("position", pos);
        this.mArgument.putInt("scroll_to_position", this.scrollToPos);
        enumSongsBundle.putString("type", "container");
        this.mContainerClickCallback.onContainerItemClick(enumSongsBundle);
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
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), getSpan(), RecyclerView.VERTICAL, false));
        this.mRecyclerView.setAdapter(this.containerGridAdapter);
        this.containerGridAdapter.addEmptyView(this.mEmptyView, false);
        this.containerGridAdapter.setIsListMode(false);
        this.containerGridAdapter.setFastScroller(this.mFastScroller);
        this.containerGridAdapter.setOnItemClickListener(new AbsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int i) {
                HomePageDefaultGenreFragment.this.onItemClick(i, false);
            }
        });
        this.mTitleView = this.mContentView.findViewById(R.id.content_title);
        if (StateManager.getInstance().isMobileLayout() && !TextUtils.isEmpty(this.mTitle)) {
            this.mTitleView.setVisibility(View.VISIBLE);
            this.mTitleView.setText(this.mTitle);
        }
        onConfigurationChanged(this.mActivity.getResources().getConfiguration());
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.containerGridAdapter.setSpan(getSpan());
    }

    @Override
    public void loadContent(final boolean refresh) {
        SynoLog.d(LOG, this.mType.name() + " loadContent(" + refresh + ")");
        if (this.loadContentWork != null && this.loadContentWork.isWorking()) {
            this.loadContentWork.endThread();
        }
        this.loadContentWork = new ThreadWork() {
            Common.ConnectionInfo connectionInfo = Common.ConnectionInfo.ERROR_NETWORK;
            List<Item> retItems = new LinkedList<>();

            @Override
            public void preWork() {
                HomePageDefaultGenreFragment.this.getActivity().setProgressBarIndeterminateVisibility(true);
                if (refresh) {
                    HomePageDefaultGenreFragment.this.page = 0;
                    HomePageDefaultGenreFragment.this.mItems.clear();
                }
                HomePageDefaultGenreFragment.this.page++;
                if (1 == HomePageDefaultGenreFragment.this.page) {
                    HomePageDefaultGenreFragment.this.setNoDataView();
                    HomePageDefaultGenreFragment.this.setRefreshing(true);
                }
            }

            @Override
            public void onWorking() {
                try {
                    CacheManager.ItemSet<Item> itemSetDoEnumContainerForContainer =
                            HomePageDefaultGenreFragment.this.cacheMgr
                                    .doEnumContainerForContainer(
                                            HomePageDefaultGenreFragment.this.isOnline,
                                            HomePageDefaultGenreFragment.this.mType,
                                            HomePageDefaultGenreFragment.this.mArgument,
                                            HomePageDefaultGenreFragment.this.page,
                                            refresh
                                    );
                    this.retItems = itemSetDoEnumContainerForContainer.getItemList();
                    HomePageDefaultGenreFragment.this.total = itemSetDoEnumContainerForContainer.getTotal();
                    this.connectionInfo = Common.ConnectionInfo.SUCCESS;
                } catch (WebAPIErrorException e) {
                    this.setException(e);
                }
            }

            @Override
            public void postWork() {
                if (1 == HomePageDefaultGenreFragment.this.page) {
                    HomePageDefaultGenreFragment.this.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                HomePageDefaultGenreFragment.this.setRefreshing(false);
                if (this.getException() != null) {
                    HomePageDefaultGenreFragment.this.handleError(this.getException());
                    if (HomePageDefaultGenreFragment.this.containerGridAdapter.getRealItemCount() == 0) {
                        HomePageDefaultGenreFragment.this.containerGridAdapter.setData(null);
                        return;
                    }
                    return;
                }
                HomePageDefaultGenreFragment.this.getActivity().setProgressBarIndeterminateVisibility(false);
                if (this.connectionInfo == Common.ConnectionInfo.SUCCESS) {
                    HomePageDefaultGenreFragment.this.mItems.addAll(this.retItems);
                    if (HomePageDefaultGenreFragment.this.mItems.isEmpty()) {
                        HomePageDefaultGenreFragment.this.setNoDataView();
                    }
                    HomePageDefaultGenreFragment.this.containerGridAdapter.setData(HomePageDefaultGenreFragment.this.mItems);
                    HomePageDefaultGenreFragment.this.mRecyclerView.post(() -> HomePageDefaultGenreFragment.this.mRecyclerView.scrollToPosition(HomePageDefaultGenreFragment.this.scrollToPos));
                    HomePageDefaultGenreFragment.this.mContainerClickCallback.onUpdateTitle();
                    HomePageDefaultGenreFragment.this.mContainerClickCallback.onFinishLoading(HomePageDefaultGenreFragment.this.mType, HomePageDefaultGenreFragment.this.total);
                    if (!HomePageDefaultGenreFragment.this.canLoadMore() || this.retItems.isEmpty()) {
                        return;
                    }
                    HomePageDefaultGenreFragment.this.loadContent(false);
                }
            }
        };
        this.loadContentWork.startWork();
    }

    private Bundle getEnumSongsBundle(Item item) {
        Bundle bundle = new Bundle();
        bundle.putString("title", item.getTitle());
        if (item.isWithRating()) {
            bundle.putFloat(SongItem.SQL_RATING, item.getRating());
        }
        bundle.putBoolean(PinManager.MODE, this.isOnline);
        Bundle bundle2 = new Bundle();
        bundle2.putString("album_artist", item.getAlbumArtist());
        bundle2.putString(PinManager.DISPLAY_ARTIST, item.getDisplayArtist());
        bundle2.putString("title", item.getTitle());
        bundle2.putString("id", item.getID());
        bundle.putBundle("ui_info", bundle2);
        bundle.putString(PinManager.GENRE_FILTER, item.getID());
        bundle.putString(Common.CONTAINER_TYPE, Common.ContainerType.GENRE_ARTIST_MODE.name());
        return bundle;
    }

    @Override
    public void onContainerItemClick(Bundle bundle) {
        this.contentBundle.putInt("position", bundle.getInt("position"));
        this.mContainerClickCallback.onContainerItemClick(this.contentBundle);
    }

    @Override
    public void onUpdateTitle() {
        this.mContainerClickCallback.onUpdateTitle();
    }

    @Override
    public void scrollToTop() { }
}
