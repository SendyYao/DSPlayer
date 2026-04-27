package com.whisperyao.dsplayer.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.adapters.AbsAdapter;
import com.whisperyao.dsplayer.adapters.RatingAdapter;
import com.whisperyao.dsplayer.fragment.ContentFragment;
import com.whisperyao.dsplayer.fragment.RatingFragment;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.widget.SynoFastScroller;
import java.util.ArrayList;
import java.util.Stack;


public class RatingFragment extends ContentFragment implements ContentFragment.ContentCallback {
    public static final int CAT_GENERAL = 0;
    public static final int CAT_PERSONAL = 1;
    public static final int CAT_SHARED = 2;
    private static final String LOG = "RatingFragment";
    private Bundle contentBundle;
    private boolean isLeft;
    private ContentFragment mContentFrag;
    private RatingAdapter ratinglistAdapter;
    private int selPos;

    @Override
    protected boolean canLoadMore() {
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
    public void onFinishLoading(Common.ContainerType type, int size) {
    }

    @Override
    public void onPageSelected() {
    }

    @Override
    public void toggleView() { }

    @Override
    public void scrollToTop() {

    }

    public static class RatingLevelItem {
        int mIconResId;
        int mStars;
        int mTtitleResId;

        public RatingLevelItem(int stars) {
            int i;
            int i2;
            int i3;
            int i4;
            this.mStars = stars;
            if (stars == 0) {
                i = R.drawable.thumbnail_rating0;
                i2 = R.string.rating_0_star;
            } else if (stars == 1) {
                i = R.drawable.thumbnail_rating1;
                i2 = R.string.rating_1_star;
            } else if (stars == 2) {
                i = R.drawable.thumbnail_rating2;
                i2 = R.string.rating_2_star;
            } else if (stars == 3) {
                i = R.drawable.thumbnail_rating3;
                i2 = R.string.rating_3_star;
            } else if (stars == 4) {
                i = R.drawable.thumbnail_rating4;
                i2 = R.string.rating_4_star;
            } else {
                if (stars != 5) {
                    i4 = 0;
                    i3 = 0;
                    this.mTtitleResId = i4;
                    this.mIconResId = i3;
                }
                i = R.drawable.thumbnail_rating5;
                i2 = R.string.rating_5_star;
            }
            int i5 = i2;
            i3 = i;
            i4 = i5;
            this.mTtitleResId = i4;
            this.mIconResId = i3;
        }

        public int getTitleId() {
            return this.mTtitleResId;
        }

        public int getIconResId() {
            return this.mIconResId;
        }

        int getStars() {
            return this.mStars;
        }
    }

    public RatingFragment() {
        this.isLeft = true;
        this.mContentFrag = null;
        this.selPos = 0;
    }

    public RatingFragment(ContentFragment.ContentCallback callback) {
        super(callback);
        this.isLeft = true;
        this.mContentFrag = null;
        this.selPos = 0;
    }

    public RatingFragment(ContentFragment.ContentCallback callback, boolean doRefresh) {
        super(callback);
        this.isLeft = true;
        this.mContentFrag = null;
        this.selPos = 0;
        this.blDoRefresh = doRefresh;
    }

    @Override
    public boolean isPlayable() {
        ContentFragment contentFragment;
        if (!this.isLeft || (contentFragment = this.mContentFrag) == null) {
            return false;
        }
        return contentFragment.isPlayable();
    }

    @Override
    public boolean canMultiEdit() {
        ContentFragment contentFragment;
        if (!this.isLeft || (contentFragment = this.mContentFrag) == null) {
            return false;
        }
        return contentFragment.canMultiEdit();
    }

    @Override
    public void markAllItem(boolean mark) {
        ContentFragment contentFragment = this.mContentFrag;
        if (contentFragment != null) {
            contentFragment.markAllItem(mark);
        }
    }

    @Override
    public void allItemPlayAction(Common.ItemAction action) {
        ContentFragment contentFragment;
        if (!this.isLeft || (contentFragment = this.mContentFrag) == null) {
            return;
        }
        contentFragment.allItemPlayAction(action);
    }

    @Override
    public void setEditMode(final boolean edit) {
        ContentFragment contentFragment;
        if (!this.isLeft || (contentFragment = this.mContentFrag) == null) {
            return;
        }
        contentFragment.setEditMode(edit);
    }

    @Override
    public boolean isEditMode() {
        ContentFragment contentFragment;
        if (!this.isLeft || (contentFragment = this.mContentFrag) == null) {
            return false;
        }
        return contentFragment.isEditMode();
    }

    @Override
    public ArrayList<SongItem> getSelectedItems() {
        ContentFragment contentFragment;
        if (this.isLeft && (contentFragment = this.mContentFrag) != null) {
            return contentFragment.getSelectedItems();
        }
        return new ArrayList<>();
    }

    @Override
    protected void onScrollToBottom(AbsListView view) {
        if (this.loadContentWork != null) {
            this.loadContentWork.isWorking();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SynoLog.d(LOG, "onCreate");
        this.mType = Common.ContainerType.RATING_MODE;
        this.isOnline = this.mArgument.getBoolean(PinManager.MODE);
        if (this.mArgument.containsKey("left_pane")) {
            this.isLeft = this.mArgument.getBoolean("left_pane");
        } else {
            this.isLeft = true;
        }
        if (this.mArgument.containsKey("position")) {
            this.selPos = this.mArgument.getInt("position");
        }
        this.ratinglistAdapter = new RatingAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SynoLog.d(LOG, "onCreateView");
        if (StateManager.getInstance().isMobileLayout() || !this.isLeft) {
            this.mContentView = inflater.inflate(R.layout.content_fragment, null);
        } else {
            this.mContentView = inflater.inflate(R.layout.tablet_content_fragment_recycler, null);
        }
        setupViews();
        if (!this.isInitialized()) {
            loadContent(this.blDoRefresh);
            this.setInitialized(true);
            this.blDoRefresh = false;
        } else if (this.ratinglistAdapter.getCount() == 0) {
            this.mEmptyView.setVisibility(0);
            setNoDataView();
        }
        return this.mContentView;
    }

    @Override
    public void onDestroyView() {
        SynoLog.d(LOG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SynoLog.d(LOG, "onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SynoLog.d(LOG, "onOptionsItemSelected : " + ((Object) item.getTitle()));
        return super.onOptionsItemSelected(item);
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
        this.mEmptyView.setVisibility(View.VISIBLE);
        TextView textView = this.mContentView.findViewById(R.id.content_title);
        if (textView != null) {
            textView.setVisibility(View.GONE);
        }
        this.mFastScroller = this.mContentView.findViewById(R.id.fast_scroller);
        this.mRecyclerView = this.mContentView.findViewById(R.id.recycler_view);
        this.mRecyclerView.setVisibility(View.VISIBLE);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), 1, false));
        this.mRecyclerView.setAdapter(this.ratinglistAdapter);
        this.ratinglistAdapter.addEmptyView(this.mEmptyView, false);
        this.ratinglistAdapter.setIsListMode(true);
        this.ratinglistAdapter.setFastScroller(this.mFastScroller);
        this.ratinglistAdapter.setOnItemClickListener(new AbsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int i) {
                if (R.id.shortcut == view.getId()) {
                    getQuickAction(view, (RatingFragment.RatingLevelItem) obj).show();
                } else {
                    RatingFragment.this.onItemClick(i, (RatingFragment.RatingLevelItem) obj, false);
                }
            }
        });
    }


    private void onItemClick(int pos, RatingLevelItem item, boolean doRefresh) {
        SynoLog.d(LOG, "onItemClick : " + pos);
        ContentFragment contentFragment = this.mContentFrag;
        if (contentFragment == null || !contentFragment.isEditMode()) {
            this.selPos = pos;
            if (StateManager.getInstance().isMobileLayout()) {
                this.mContainerClickCallback.onContainerItemClick(getRatingSongBundle(item));
            } else if (this.isLeft) {
                Bundle ratingSongBundle = getRatingSongBundle(item);
                this.contentBundle = (Bundle) ratingSongBundle.clone();
                ratingSongBundle.putBoolean("left_pane", false);
                this.mArgument.putInt("position", pos);
                this.mContentFrag = ContentFragment.Companion.newInstance(ratingSongBundle, this, doRefresh);
                getChildFragmentManager().beginTransaction().replace(R.id.tablet_content, this.mContentFrag).commit();
            } else {
                this.mContainerClickCallback.onContainerItemClick(getRatingBundle(item));
            }
            if (StateManager.getInstance().isMobileLayout() || !this.isLeft) {
                return;
            }
            this.ratinglistAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadContent(final boolean refresh) {
        if (!StateManager.getInstance().isMobileLayout() && this.isLeft) {
            int i = this.selPos;
            onItemClick(i, this.ratinglistAdapter.getItem(i), refresh);
        }
        setRefreshing(false);
    }

    private Bundle getRatingBundle(RatingLevelItem item) {
        Bundle bundle = new Bundle();
        bundle.putString(Common.CONTAINER_TYPE, Common.ContainerType.RATING_MODE.name());
        bundle.putString("type", "container");
        bundle.putBoolean("left_pane", true);
        bundle.putInt("position", this.selPos);
        return bundle;
    }

    private Bundle getRatingSongBundle(RatingLevelItem item) {
        Bundle bundle = new Bundle();
        bundle.putString(Common.CONTAINER_TYPE, Common.ContainerType.RATING_MODE.name());
        bundle.putString("type", Common.ContainerType.RATING_MODE.name());
        bundle.putBoolean(PinManager.MODE, this.isOnline);
        bundle.putString("key", Common.CAT_RANDOM100_ID);
        bundle.putString("title", getString(item.getTitleId()));
        bundle.putInt("song_rating_level", item.getStars());
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

    private void onRatingLevelClicked(int itemAction, final RatingLevelItem item) {
        if (itemAction == R.id.ItemAction_ADD_ITEM) {
            enumSongs(Common.PlaybackAction.ADD_ONLY, item);
        } else if (itemAction == R.id.ItemAction_ADD_NEXT) {
            enumSongs(Common.PlaybackAction.ADD_NEXT, item);
        } else if (itemAction == R.id.ItemAction_PLAY) {
            enumSongs(Common.PlaybackAction.PLAY_NOW, item);
        }
    }

    private void enumSongs(final Common.PlaybackAction action, final RatingLevelItem item) {
        final ProgressDialog progressDialog = new ProgressDialog(this.mActivity);
        progressDialog.setMessage(getResources().getString(R.string.processing));
        progressDialog.setCancelable(false);
        new ThreadWork() {
            ArrayList<SongItem> songList = new ArrayList<>();

            @Override
            public void preWork() {
                progressDialog.show();
            }

            @Override
            public void onWorking() {
                try {
                    Bundle ratingSongBundle = RatingFragment.this.getRatingSongBundle(item);
                    this.songList.addAll(RatingFragment.this.cacheMgr
                            .doEnumContainerSongsForContainer(
                                    ratingSongBundle.getBoolean(PinManager.MODE),
                                    Common.ContainerType.valueOf(ratingSongBundle.getString(Common.CONTAINER_TYPE)),
                                    ratingSongBundle,
                                    -1,
                                    true)
                            .getItemList());
                } catch (WebAPIErrorException e) {
                    this.setException(e);
                }
            }

            @Override
            public void onComplete() {
                progressDialog.dismiss();
                if (this.getException() != null) {
                    RatingFragment.this.handleError(this.getException());
                    if (RatingFragment.this.ratinglistAdapter.getRealItemCount() == 0) {
                        RatingFragment.this.ratinglistAdapter.setData(null);
                        return;
                    }
                    return;
                }
                RatingFragment.this.enqueueAction(action, 0, this.songList);
            }
        }.startWork();
    }

    private PopupMenu getQuickAction(final View anchor, final RatingLevelItem item) {
        PopupMenu popupMenu = new PopupMenu(getContext(), anchor);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            onRatingLevelClicked(menuItem.getItemId(), item);
            return false;
        });
        popupMenu.inflate(R.menu.rating_menu);
        if (ConnectionManager.canSupportAddToNext()) {
            popupMenu.getMenu().findItem(R.id.ItemAction_ADD_NEXT).setVisible(true);
        }
        return popupMenu;
    }

}
