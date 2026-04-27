package com.whisperyao.dsplayer.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.adapters.RadioListAdapter;
import com.whisperyao.dsplayer.fragment.ContentFragment;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.widget.SynoFastScroller;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class RadioFragment extends ContentFragment implements ContentFragment.ContentCallback {
    private static final String API_SHOUTcastID = "SHOUTcast";
    private static final String CGI_SHOUTcastID = "inetradio_sc";
    private static final String LOG = "RadioFragment";
    private static final String RADIO_ID = "radio_id";
    private static final String SHOUTCAST = "shoutcast";
    private Bundle contentBundle;
    private boolean hasShoutcast;
    private boolean isLeft;
    private ContentFragment mContentFrag;
    private ArrayList<SongItem> mItems;
    private String mKey;
    private String mRadioId;
    private ImageView mShoutCastLogo;
    private int page;
    private RadioListAdapter radiolistAdapter;
    private int scrollToPos;
    private int selPos;

    @Override
    public void allItemPlayAction(Common.ItemAction action) {
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
    public void onFinishLoading(Common.ContainerType type, int size) { }

    @Override
    protected void onPageSelected() { }

    @Override
    public void setEditMode(boolean edit) { }

    @Override
    public void toggleView() { }

    public RadioFragment() {
        this.mItems = new ArrayList<>();
        this.mRadioId = "";
        this.page = 0;
        this.hasShoutcast = false;
        this.isLeft = true;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
    }

    public RadioFragment(ContentFragment.ContentCallback callback) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.mRadioId = "";
        this.page = 0;
        this.hasShoutcast = false;
        this.isLeft = true;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
    }

    public RadioFragment(ContentFragment.ContentCallback callback, boolean doRefresh) {
        super(callback);
        this.mItems = new ArrayList<>();
        this.mRadioId = "";
        this.page = 0;
        this.hasShoutcast = false;
        this.isLeft = true;
        this.selPos = -1;
        this.scrollToPos = -1;
        this.mContentFrag = null;
        this.blDoRefresh = doRefresh;
    }

    @Override
    protected void doRefresh() {
        SynoLog.d(LOG, "doRefresh");
        loadContent(true);
        ContentFragment contentFragment = this.mContentFrag;
        if (contentFragment != null) {
            contentFragment.doRefresh();
        }
    }

    @Override
    public ArrayList<SongItem> getSelectedItems() {
        return new ArrayList<>();
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
        this.mType = Common.ContainerType.RADIO_MODE;
        this.isOnline = true;
        this.page = 0;
        this.mItems = new ArrayList<>();
        this.mKey = this.mArgument.getString("key");
        if (this.mArgument.containsKey("title")) {
            this.mTitle = this.mArgument.getString("title");
        }
        if (this.mArgument.containsKey("left_pane")) {
            this.isLeft = this.mArgument.getBoolean("left_pane");
        } else {
            this.isLeft = true;
        }
        if (this.mArgument.containsKey("position")) {
            this.selPos = this.mArgument.getInt("position");
            this.scrollToPos = this.mArgument.getInt("scroll_to_position");
        }
        if (this.mArgument.containsKey(RADIO_ID)) {
            this.mRadioId = this.mArgument.getString(RADIO_ID);
        } else {
            this.mRadioId = "";
        }
        if (this.mArgument.containsKey(SHOUTCAST)) {
            this.hasShoutcast = this.mArgument.getBoolean(SHOUTCAST);
        }
        if (this.mArgument.containsKey("scroll_state")) {
            this.mScrollState = this.mArgument.getParcelable("scroll_state");
        }
        RadioListAdapter radioListAdapter = new RadioListAdapter();
        this.radiolistAdapter = radioListAdapter;
        radioListAdapter.setIsListMode(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SynoLog.d(LOG, "onCreateView");
        if (StateManager.getInstance().isMobileLayout() || !this.isLeft) {
            this.mContentView = inflater.inflate(R.layout.content_fragment, (ViewGroup) null);
        } else {
            this.mContentView = inflater.inflate(R.layout.tablet_content_fragment_recycler, (ViewGroup) null);
        }
        setupViews();
        if (!this.isInitialized()) {
            loadContent(this.blDoRefresh);
            this.setInitialized(true);
            this.blDoRefresh = false;
        } else if (this.mItems.isEmpty()) {
            setNoDataView();
        }
        return this.mContentView;
    }

    @Override
    public void loadContent(final boolean refresh) {
        SynoLog.d(LOG, "loadContent(" + refresh + "), type = " + this.mType.name());
        if (this.loadContentWork != null && this.loadContentWork.isWorking()) {
            this.loadContentWork.endThread();
        }
        this.loadContentWork = new ThreadWork() {
            Common.ConnectionInfo connectionInfo;
            List<SongItem> retItems = new LinkedList<>();

            @Override
            public void preWork() {
                RadioFragment.this.getActivity().setProgressBarIndeterminateVisibility(true);
                if (refresh) {
                    RadioFragment.this.page = 0;
                    RadioFragment.this.mItems.clear();
                }
                RadioFragment.this.page++;
                if (1 == RadioFragment.this.page) {
                    RadioFragment.this.setRefreshing(true);
                }
            }

            @Override
            public void onWorking() {
                try {
                    CacheManager.ItemSet<SongItem> itemSetDoEnumRadiosForRadios =
                            RadioFragment.this.cacheMgr
                                    .doEnumRadiosForRadios(
                                            RadioFragment.this.mKey,
                                            RadioFragment.this.page,
                                            refresh
                                    );
                    this.retItems = itemSetDoEnumRadiosForRadios.getItemList();
                    RadioFragment.this.total = itemSetDoEnumRadiosForRadios.getTotal();
                    this.connectionInfo = Common.ConnectionInfo.SUCCESS;
                } catch (WebAPIErrorException e) {
                    this.setException(e);
                }
            }

            @Override
            public void postWork() {
                if (1 == RadioFragment.this.page) {
                    RadioFragment.this.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                RadioFragment.this.getActivity().setProgressBarIndeterminateVisibility(false);
                if (this.getException() != null) {
                    RadioFragment.this.handleError(this.getException());
                    if (RadioFragment.this.radiolistAdapter.getRealItemCount() == 0) {
                        RadioFragment.this.radiolistAdapter.setData(null);
                        return;
                    }
                    return;
                }
                if (this.connectionInfo == Common.ConnectionInfo.SUCCESS) {
                    RadioFragment.this.mItems.addAll(this.retItems);
                    RadioFragment.this.radiolistAdapter.setData(RadioFragment.this.mItems);
                    if (RadioFragment.this.mItems.isEmpty()) {
                        RadioFragment.this.setNoDataView();
                    } else if (RadioFragment.this.mItems.get(0).getType().equals(Item.ItemType.CONTAINER_MODE)) {
                        RadioFragment.this.mRecyclerView.post(() -> RadioFragment.this.mRecyclerView.scrollToPosition(RadioFragment.this.scrollToPos));
                    }
                    RadioFragment.this.mContainerClickCallback.onUpdateTitle();
                    if (!StateManager.getInstance().isMobileLayout() && RadioFragment.this.isLeft && !RadioFragment.this.mItems.isEmpty()) {
                        if (RadioFragment.this.selPos >= RadioFragment.this.mItems.size()) {
                            if (RadioFragment.this.canLoadMore()) {
                                RadioFragment.this.loadContent(false);
                                return;
                            } else {
                                RadioFragment.this.onItemClick(mItems.size() - 1, -1, refresh);
                            }
                        } else if (RadioFragment.this.selPos < 0) {
                            RadioFragment.this.onItemClick(0, -1, refresh);
                        } else {
                            RadioFragment radioFragment = RadioFragment.this;
                            radioFragment.onItemClick(radioFragment.selPos, -1, refresh);
                        }
                    }
                    if (!RadioFragment.this.canLoadMore() || this.retItems.size() == 0) {
                        return;
                    }
                    RadioFragment.this.loadContent(false);
                }
            }
        };
        this.loadContentWork.startWork();
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
        this.mTitleView = this.mContentView.findViewById(R.id.content_title);
        if (StateManager.getInstance().isMobileLayout() && !TextUtils.isEmpty(this.mTitle)) {
            this.mTitleView.setVisibility(0);
            this.mTitleView.setText(this.mTitle);
        }
        ImageView imageView = this.mContentView.findViewById(R.id.Shoutcast_Logo);
        this.mShoutCastLogo = imageView;
        if (!this.isLeft) {
            imageView.setVisibility(View.GONE);
        } else if (this.hasShoutcast) {
            imageView.setVisibility(View.VISIBLE);
            this.mShoutCastLogo.setOnClickListener(v -> RadioFragment.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(Common.SHOUTCAST_URL))));
        }
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), 1, false));
        this.mRecyclerView.setAdapter(this.radiolistAdapter);
        this.radiolistAdapter.addEmptyView(this.mEmptyView, false);
        this.radiolistAdapter.setFastScroller(this.mFastScroller);
        this.radiolistAdapter.setOnItemClickListener(new AbsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int i) {
                if (R.id.shortcut == view.getId() || R.id.SongItemShortCut == view.getId()) {
                    getQuickAction(view, (SongItem) obj).show();
                } else {
                    RadioFragment.this.onItemClick(i, radiolistAdapter.getScrollToPosition(), false);
                }
            }
        });
    }


    private void onItemClick(int pos, int first, boolean doRefresh) {
        SongItem songItem = this.radiolistAdapter.getData().get(pos);
        this.selPos = pos;
        if (first >= 0) {
            this.scrollToPos = first;
        }
        if (Item.ItemType.CONTAINER_MODE == songItem.getType()) {
            Bundle bundle = new Bundle();
            bundle.putString(Common.CONTAINER_TYPE, this.mType.name());
            bundle.putString("key", songItem.getID());
            bundle.putString("title", songItem.getTitle());
            bundle.putInt("position", pos);
            bundle.putInt("scroll_to_position", this.scrollToPos);
            this.mArgument.putInt("scroll_to_position", this.scrollToPos);
            if (TextUtils.isEmpty(this.mRadioId)) {
                bundle.putString(RADIO_ID, songItem.getID());
            } else {
                bundle.putString(RADIO_ID, this.mRadioId);
            }
            bundle.putBoolean(SHOUTCAST, this.hasShoutcast || API_SHOUTcastID.equals(songItem.getID()) || CGI_SHOUTcastID.equals(songItem.getID()));
            if (StateManager.getInstance().isMobileLayout() || !this.isLeft) {
                this.mContainerClickCallback.onContainerItemClick(bundle);
            } else {
                this.contentBundle = (Bundle) bundle.clone();
                bundle.putBoolean("left_pane", false);
                this.mArgument.putInt("position", pos);
                this.mContentFrag = ContentFragment.Companion.newInstance(bundle, this, doRefresh);
                getChildFragmentManager().beginTransaction().replace(R.id.tablet_content, this.mContentFrag).commit();
            }
        } else {
            Common.PlaybackAction playbackAction = Common.PlaybackAction.PLAY_NOW;
            if (Common.TapSongAction.ADD.equals(AudioPreference.getTapSongPref())) {
                enumRadios(Common.PlaybackAction.BY_SITUACTION, songItem);
            } else {
                enumRadios(playbackAction, songItem, false);
            }
        }
        if (StateManager.getInstance().isMobileLayout() || !this.isLeft) {
            return;
        }
        if (this.selPos == 0) {
            this.mShoutCastLogo.setVisibility(0);
        } else {
            this.mShoutCastLogo.setVisibility(8);
        }
        this.radiolistAdapter.notifyDataSetChanged();
    }

    private PopupMenu getQuickAction(final View anchor, final SongItem song) {
        PopupMenu popupMenu = getPopupMenu(anchor, song);
        popupMenu.inflate(R.menu.radio_menu);
        if (song.isRadio()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_PLAY).setVisible(true);
            popupMenu.getMenu().findItem(R.id.ItemAction_ADD_ITEM).setVisible(true);
            if (Common.editPersonalPlaylist()) {
                popupMenu.getMenu().findItem(R.id.ItemAction_ADDTO_PLAYLIST).setVisible(true);
            }
        }
        if (ConnectionManager.canShareSong(true, song)) {
            popupMenu.getMenu().findItem(R.id.ItemAction_SHARING).setVisible(true);
        }
        if (Common.getDsId().equals(song.getDsId()) && Common.createPersonalPlaylist()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_ADDTO_PLAYLIST).setVisible(true);
        }
        return popupMenu;
    }

    @NonNull
    private PopupMenu getPopupMenu(View anchor, SongItem song) {
        PopupMenu popupMenu = new PopupMenu(getContext(), anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.ItemAction_ADDTO_PLAYLIST) {
                listPlaylistOption(song);
            } else if (itemId == R.id.ItemAction_ADD_ITEM) {
                enumRadios(Common.PlaybackAction.ADD_ONLY, song);
            } else if (itemId == R.id.ItemAction_PLAY) {
                enumRadios(Common.PlaybackAction.PLAY_NOW, song);
            }
            return false;
        });
        return popupMenu;
    }

    private void enumRadios(final Common.PlaybackAction action, final SongItem item) {
        enumRadios(action, item, true);
    }

    private void enumRadios(final Common.PlaybackAction action, final SongItem item, final boolean isFromMenu) {
        ArrayList<SongItem> arrayList = new ArrayList<>();
        arrayList.add(item);
        enqueueAction(action, 0, arrayList, isFromMenu);
    }

    @Override // com.synology.dsaudio.fragment.ContentFragment.ContentCallback
    public void onContainerItemClick(Bundle bundle) {
        this.contentBundle.putInt("position", bundle.getInt("position"));
        this.contentBundle.putInt("scroll_to_position", bundle.getInt("scroll_to_position"));
        this.mContainerClickCallback.onContainerItemClick(this.contentBundle);
    }

    @Override // com.synology.dsaudio.fragment.ContentFragment.ContentCallback
    public void onUpdateTitle() {
        this.mContainerClickCallback.onUpdateTitle();
    }

    @Override
    public void scrollToTop() {

    }
}
