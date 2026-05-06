package com.whisperyao.dsplayer.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.adapters.HomePinAdapter;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.HomePagePinItem;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;

public class HomePagePinsFragment extends ContentFragment implements ContentFragment.ContentCallback, PinManager.Callback {
    private static final String LOG = "HomePagePinsFragment";
    private HomePinAdapter containerGridAdapter;
    private Bundle contentBundle;
    private ThreadWork enumSongsWork;
    private ContentFragment mContentFrag;
    private ArrayList<HomePagePinItem> mItems;
    private PinManager mPinManager = null;
    private int page;
    private int scrollToPos;
    private int selPos;

    @Override
    public void allItemPlayAction(Common.ItemAction action) {
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
    public void onFinishLoading(Common.ContainerType type, int size) {
    }

    @Override
    public void onPinErrorOccur() {
    }

    @Override
    public void setEditMode(final boolean edit) {
    }

    @Override
    public void toggleView() {
    }

    public HomePagePinsFragment() {
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
    }

    public HomePagePinsFragment(ContentCallback callback, boolean load) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
        this.blLoadContent = load;
        this.blDoRefresh = true;
    }

    public HomePagePinsFragment(ContentCallback callback, boolean load, boolean doRefresh) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.page = 0;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
        this.blLoadContent = load;
        this.blDoRefresh = doRefresh;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SynoLog.d(LOG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.isOnline = this.getArguments().getBoolean(PinManager.MODE);
        this.mType = Common.ContainerType.HOMEPAGE_PIN_MODE;
        if (this.mArgument.containsKey("title")) {
            this.mTitle = Objects.requireNonNull(this.mArgument.getString("title"));
        }
        if (this.mArgument.containsKey("position")) {
            this.selPos = this.mArgument.getInt("position");
            this.scrollToPos = this.mArgument.getInt("scroll_to_position");
        } else {
            this.selPos = 0;
            this.scrollToPos = 0;
        }
        this.containerGridAdapter = new HomePinAdapter();
        this.mPinManager = PinManager.Companion.getInstance();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SynoLog.d("LIFECYCLE", "onAttach: " + this);
        Fragment parent = getParentFragment();

        if (parent instanceof ContentCallback) {
            mContainerClickCallback = (ContentCallback) parent;
            SynoLog.d("DEBUG", "Callback绑定成功: " + parent.getClass().getName());
        } else {
            SynoLog.d("DEBUG", "Callback绑定失败");
        }
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
    public void onDestroyView() {
        this.mPinManager.removeCallback(this);
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        if (this.blLoadContent) {
            onPageSelected();
        }
        this.mContainerClickCallback.onUpdateTitle();
        this.mPinManager.addCallback(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mContentView = inflater.inflate(R.layout.content_fragment, null);
        return this.mContentView;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        SynoLog.d(LOG, "onOptionsItemSelected : " + item.getTitle());
        if (item.getItemId() == R.id.menu_edit) {
            new HomePagePinReorderFragment().show(getChildFragmentManager(), "pin_reorder");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canMultiEdit() {
        ArrayList<HomePagePinItem> arrayList = this.mItems;
        return arrayList != null && !arrayList.isEmpty();
    }

    @Override
    public void markAllItem(boolean mark) {
        ContentFragment contentFragment = this.mContentFrag;
        if (contentFragment != null) {
            contentFragment.markAllItem(mark);
        }
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
    public void onPinPreLoading() {
        this.mItems.clear();
        this.mEmptyView.setVisibility(View.GONE);
        setNoDataView();
        setRefreshing(true);
    }

    @Override
    public void onPinLoadFinish() {
        WebAPIErrorException webAPIErrorException = this.mPinManager.getWebAPIErrorException();
        if (webAPIErrorException != null) {
            handleError(webAPIErrorException);
            return;
        }
        this.mItems.addAll(this.mPinManager.getItems());
        setRefreshing(false);
        this.containerGridAdapter.setData(this.mItems);
        this.mRecyclerView.post(() -> {
            this.mRecyclerView.scrollToPosition(scrollToPos);
        });
        if (this.mItems.isEmpty()) {
            SynoLog.e(LOG, "onPinLoadFinish this.mItems.isEmpty() = true");
            setNoDataView();
            if (this.mContentFrag != null) {
                getChildFragmentManager().beginTransaction().remove(this.mContentFrag).commit();
            }
        }
        SynoLog.d("HomePagePinsFragment", this.mItems.toString());
        this.containerGridAdapter.setData(this.mItems);
        this.mContainerClickCallback.onUpdateTitle();
        this.mContainerClickCallback.onFinishLoading(this.mType, this.total);
    }

    @Override
    protected void setNoDataView() {
        this.mEmptyImageView.setImageResource(R.drawable.nodata_pin);
        this.mEmptyTextView.setText(R.string.no_data_pin);
    }

    private void onItemClick(int pos, boolean doRefresh) {
        SynoLog.d(LOG, "onItemClick : " + pos);
        ContentFragment contentFragment = this.mContentFrag;
        if (contentFragment == null || !contentFragment.isEditMode()) {
            this.selPos = pos;
            this.scrollToPos = this.containerGridAdapter.getScrollToPosition();
            this.mArgument.putInt("position", this.selPos);
            this.mArgument.putInt("scroll_to_position", this.scrollToPos);
            SynoLog.d(LOG, "EnumSongsBundle: " + PinManager.Companion.getEnumSongsBundle(this.containerGridAdapter.getData().get(pos)));
            SynoLog.d("DEBUG", "callback = " + mContainerClickCallback.getClass().getName());
            this.mContainerClickCallback.onContainerItemClick(PinManager.Companion.getEnumSongsBundle(this.containerGridAdapter.getData().get(pos)));
        }
    }

    private void setupViews() {
        this.mLoadingView = this.mContentView.findViewById(R.id.content_progress);
        this.mRefresh = this.mContentView.findViewById(R.id.refresh);
        this.mRefresh.setOnRefreshListener(() -> {
            if (this.loadContentWork == null ||!this.loadContentWork.isWorking()) {
                this.scrollToPos = 0;
                doRefresh();
            }
        });
        this.mRefresh.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        this.mEmptyView = this.mContentView.findViewById(R.id.content_empty);
        this.mEmptyTextView = this.mContentView.findViewById(R.id.tv_no_data);
        this.mEmptyImageView = this.mContentView.findViewById(R.id.icon_no_data);
        this.mEmptyImageView.setImageResource(R.drawable.wizard_1);
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), getSpan(), RecyclerView.VERTICAL, false));
        this.mRecyclerView.setAdapter(this.containerGridAdapter);
        this.containerGridAdapter.addEmptyView(this.mEmptyView, false);
        this.containerGridAdapter.setIsListMode(false);
        this.containerGridAdapter.setFastScroller(this.mFastScroller);
        this.containerGridAdapter.setOnItemClickListener((view, obj, i) -> {

            if (R.id.shortcut == view.getId()) {
                SynoLog.d("HomePagePinsFragment", "直接getQuickAction");
                getQuickAction(view, obj).show();
            } else {
                SynoLog.d("HomePagePinsFragment", "触发HomePagePinsFragment.this.onItemClick");
                HomePagePinsFragment.this.onItemClick(i, false);
            }
        });
        this.mTitleView = this.mContentView.findViewById(R.id.content_title);
        if (StateManager.getInstance().isMobileLayout() && !TextUtils.isEmpty(this.mTitle)) {
            this.mTitleView.setVisibility(View.VISIBLE);
            this.mTitleView.setText(this.mTitle);
        }
        onConfigurationChanged(this.getActivity().getResources().getConfiguration());
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.containerGridAdapter.setSpan(getSpan());
    }

    @Override
    public void loadContent(final boolean refresh) {
        SynoLog.d(LOG, this.mType.name() + " loadContent(" + refresh + ")");
        if (refresh) {
            System.out.println(this.mPinManager);
            if (this.mPinManager == null) {
                this.mPinManager = PinManager.Companion.getInstance();
            }
            this.mPinManager.loadPinList(true);
        } else if (this.mPinManager.isLoading()) {
            onPinPreLoading();
        } else {
            onPinLoadFinish();
        }
    }

    private PopupMenu getQuickAction(final View anchor, final HomePagePinItem pinItem) {
        PopupMenu popupMenu = getPopupMenu(anchor, pinItem);
        SynoLog.i("PopupMenu", pinItem.getType());
        if (!pinItem.getType().equals(PinManager.TYPE_RECENTLY_ADDED)) {
            popupMenu.getMenu().findItem(R.id.ItemAction_PLAY).setVisible(true);
            popupMenu.getMenu().findItem(R.id.ItemAction_ADD_ITEM).setVisible(true);
            if (ConnectionManager.canSupportAddToNext()) {
                popupMenu.getMenu().findItem(R.id.ItemAction_ADD_NEXT).setVisible(true);
            }
        }
        return popupMenu;
    }

    @NonNull
    private PopupMenu getPopupMenu(View anchor, HomePagePinItem pinItem) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.ItemAction_UNPIN) {
                this.mPinManager.unpin(pinItem.getId());
                return false;
            }
            if (menuItem.getItemId() == R.id.ItemAction_PIN_EDIT) {
                HomePagePinEditFragment.Companion.newInstance(pinItem).show(getChildFragmentManager(), "pin_edit");
                return false;
            }
            SynoLog.d(LOG, "准备enumSongs");
            enumSongs(menuItem.getItemId(), pinItem);
            return false;
        });
        popupMenu.inflate(R.menu.home_pin_menu);
        return popupMenu;
    }

    private void enumSongs(final int itemAction, final HomePagePinItem item) {
        final ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getResources().getString(R.string.processing));
        progressDialog.setCancelable(false);
        ThreadWork threadWork = new ThreadWork() {
            ArrayList<SongItem> songList = new ArrayList<>();

            @Override
            public void preWork() {
                progressDialog.show();
            }

            @Override
            public void onWorking() {
                try {
                    Common.ContainerType containerType = PinManager.Companion.getContainerTypeByItem(item);
                    SynoLog.d(LOG, "onWorking type=" + item.getType());

                    HashMap<String, String> criteria = item.getCriteria();
                    String type = Objects.toString(item.getType(), "");

                    CacheManager.ItemSet result;

                    switch (type) {
                        case "album":
                        case "artist":
                        case "composer":
                        case "genre":
                        case "random_100": {
                            CacheManager cacheMgr = HomePagePinsFragment.this.cacheMgr;
                            boolean isOnline = HomePagePinsFragment.this.isOnline;

                            Bundle bundle = PinManager.Companion.getEnumSongsBundle(item);

                            result = cacheMgr.doEnumContainerSongsForContainer(
                                    isOnline,
                                    containerType,
                                    bundle,
                                    -1,
                                    true
                            );
                            break;
                        }

                        case "playlist": {
                            String playlistId = criteria.get("playlist");

                            PlaylistItem playlist = PlaylistItem.generatePlaylistWithType(
                                    Item.ItemType.PERSONAL_NORMAL_NEW,
                                    playlistId,
                                    item.getTitle()
                            );

                            result = HomePagePinsFragment.this.cacheMgr
                                    .doEnumPlaylistSongsForPlaylist(true, playlist, -1, true);
                            break;
                        }

                        case "folder": {
                            String folderId = criteria.get("folder");

                            result = HomePagePinsFragment.this.cacheMgr
                                    .doEnumFolderSongsForFileSongList(true, folderId, true, -1, true);
                            break;
                        }

                        default: {
                            result = new CacheManager.ItemSet();
                            break;
                        }
                    }

                    // 收集结果 & 防止NPE
                    if (result != null && result.getItemList() != null) {
                        songList.addAll(result.getItemList());
                    }

                } catch (WebAPIErrorException e) {
                    setException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void postWork() {
                progressDialog.dismiss();
            }

            @Override
            public void onComplete() {
                if (this.getException() != null) {
                    HomePagePinsFragment.this.handleError(this.getException());
                    return;
                }

                if (itemAction == R.id.ItemAction_ADD_ITEM) {
                    HomePagePinsFragment.this.enqueueAction(
                            Common.PlaybackAction.ADD_ONLY, 0, this.songList);

                } else if (itemAction == R.id.ItemAction_ADD_NEXT) {
                    HomePagePinsFragment.this.enqueueAction(
                            Common.PlaybackAction.ADD_NEXT, 0, this.songList);

                } else if (itemAction == R.id.ItemAction_PLAY) {
                    HomePagePinsFragment.this.enqueueAction(
                            Common.PlaybackAction.PLAY_NOW, 0, this.songList);
                }
            }
        };

        this.enumSongsWork = threadWork;
        threadWork.startWork();
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
    public void scrollToTop() {

    }
}
