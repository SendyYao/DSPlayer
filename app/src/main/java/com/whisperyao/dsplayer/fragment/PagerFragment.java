package com.whisperyao.dsplayer.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.activity.HomeActivity;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;


public class PagerFragment extends Fragment implements ContentFragment.ContentCallback {
    private static final String KEY_FILTER_KEY = "filterKey";
    private static final String KEY_ONLINE = "online";
    private static final String KEY_PARENT = "parent";
    private static final String LOG = "PagerFragment";
    private ArrayList<Stack<Bundle>> BundleStacks;
    private Bundle currentPageBundle;
    private boolean isOnline;
    private View mContentView;
    private int mCurrentPage;
    protected Disposable mDisposableOnViewTypeChanged;
    private String mFilterKey;
    private ContentFragment[] mFragmentLists;
    private FragmentPagerAdapter mPagerAdapter;
    private PagerParent mParent;
    private TabLayout mTab;
    private ViewPager mViewPager;

    private static final Callbacks sDummyUpdateTitle = () -> {};
    private static final Common.ContainerType[] main_title_value = {Common.ContainerType.ALBUM_MODE, Common.ContainerType.ARTIST_MODE, Common.ContainerType.COMPOSER_MODE, Common.ContainerType.GENRE_MODE, Common.ContainerType.FOLDER_MODE};
    private static final Common.ContainerType[] search_title_value = {Common.ContainerType.SEARCH_ARTIST_MODE, Common.ContainerType.SEARCH_ALBUM_MODE, Common.ContainerType.SEARCH_SONG_MODE};
    private static final Common.ContainerType[] cgi_search_title_value = {Common.ContainerType.SEARCH_SONG_MODE};
    private final List<Common.ContainerType> mPageTypeList = new ArrayList<>();
    private final HashMap<Common.ContainerType, Integer> mPageTypeSizeMap = new HashMap<>();
    protected Subject<Boolean> mSubjectViewTypeChanged = PublishSubject.create();
    private Callbacks mOnUpdateTitle = sDummyUpdateTitle;
    protected final BroadcastReceiver mSongDeletedListener = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            int intExtra = intent.getIntExtra(Common.FRAGMENT_HASH, -1);
            SynoLog.d("mSongDeletedListener", "onReceive delete song from: " + intExtra);
            for (ContentFragment contentFragment : PagerFragment.this.mFragmentLists) {
                if (contentFragment.hashCode() != intExtra && (contentFragment instanceof FileSongFragment) || (contentFragment instanceof ContainerSongFragment)) {
                    contentFragment.setToNeedRefresh();
                }
            }
        }
    };
    private final TabLayout.OnTabSelectedListener mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() { // from class: com.synology.dsaudio.fragment.PagerFragment.3
        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            SynoLog.d(PagerFragment.LOG, "onPageSelected " + position);
            if (PagerFragment.this.mFragmentLists != null && PagerFragment.this.mFragmentLists[PagerFragment.this.mCurrentPage] != null && PagerFragment.this.mFragmentLists[PagerFragment.this.mCurrentPage].isEditMode()) {
                PagerFragment.this.mFragmentLists[PagerFragment.this.mCurrentPage].setEditMode(false);
            }
            PagerFragment.this.mCurrentPage = position;
            if (PagerFragment.this.mFragmentLists != null && PagerFragment.this.mFragmentLists[PagerFragment.this.mCurrentPage] != null) {
                PagerFragment.this.mFragmentLists[PagerFragment.this.mCurrentPage].onPageSelected();
            }
            if (PagerFragment.this.isOnline) {
                AudioPreference.setLibraryPage(requireContext(), PagerFragment.this.mPageTypeList.get(position));
            } else {
                AudioPreference.setLocalPage(requireContext(), PagerFragment.this.mPageTypeList.get(position));
            }
            PagerFragment.this.mOnUpdateTitle.doUpdateTitle();
        }
    };

    public interface Callbacks {
        void doUpdateTitle();
    }

    public enum PagerParent {
        MAIN_ACTIVITY,
        SEARCH_ACTIVITY
    }

    public static PagerFragment newInstance(PagerParent parent, String filterKey, Boolean online) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_ONLINE, online);
        bundle.putString(KEY_FILTER_KEY, filterKey);
        bundle.putString(KEY_PARENT, parent.name());
        PagerFragment pagerFragment = new PagerFragment();
        pagerFragment.setArguments(bundle);
        return pagerFragment;
    }

    class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            int intExtra = intent.getIntExtra(Common.FRAGMENT_HASH, -1);
            SynoLog.d("mSongDeletedListener", "onReceive delete song from: " + intExtra);
            for (ContentFragment contentFragment : PagerFragment.this.mFragmentLists) {
                if (contentFragment.hashCode() != intExtra && ((contentFragment instanceof FileSongFragment) || (contentFragment instanceof ContainerSongFragment))) {
                    contentFragment.setToNeedRefresh();
                }
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SynoLog.d(LOG, this.isOnline + " : onCreate");
        Bundle arguments = getArguments();
        this.isOnline = arguments.getBoolean(KEY_ONLINE);
        this.mParent = PagerParent.valueOf(arguments.getString(KEY_PARENT));
        this.mFilterKey = arguments.getString(KEY_FILTER_KEY);
        determinePageList();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        SynoLog.d(LOG, "onConfigurationChanged");
        for (ContentFragment contentFragment : this.mFragmentLists) {
            if (contentFragment != null) {
                contentFragment.onConfigurationChanged(newConfig);
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        SynoLog.d(LOG, this.isOnline + " : onResume");
        if (Common.gIsClearLocalCache && !this.isOnline) {
            this.mPagerAdapter.notifyDataSetChanged();
            for (ContentFragment contentFragment : this.mFragmentLists) {
                if (contentFragment != null) {
                    contentFragment.doRefresh();
                }
            }
            Common.gIsClearLocalCache = false;
        }
        if (Common.gLibraryChanged && this.isOnline) {
            CacheManager.getInstance().clearOnlineCache();
            clearStacks();
            Common.gLibraryChanged = false;
        }
        super.onResume();
    }

    private void clearStacks() {
        int size = this.mPageTypeList.size();
        for (int i = 0; i < size; i++) {
            if (this.mFragmentLists[i] != null) {
                getChildFragmentManager().beginTransaction().remove(this.mFragmentLists[i]).commitNowAllowingStateLoss();
                this.BundleStacks.get(i).clear();
                this.mFragmentLists[i] = null;
            }
        }
        TabLayout tabLayout = this.mTab;
        if (tabLayout != null) {
            tabLayout.removeOnTabSelectedListener(this.mOnTabSelectedListener);
        }
        this.mPagerAdapter.notifyDataSetChanged();
        TabLayout tabLayout2 = this.mTab;
        if (tabLayout2 != null) {
            tabLayout2.addOnTabSelectedListener(this.mOnTabSelectedListener);
        }
    }

    @Override
    public void onPause() {
        SynoLog.d(LOG, this.isOnline + " : onPause");
        super.onPause();
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        SynoLog.d(LOG, this.isOnline + " : onActivityCreated");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) throws Resources.NotFoundException {
        SynoLog.d(LOG, this.isOnline + " : onCreateView");
        this.mContentView = inflater.inflate(R.layout.pager_fragment, null);
        if (!this.isOnline) {
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(this.mSongDeletedListener, new IntentFilter(Common.ACTION_LOCAL_SONG_DELETED));
        }
        setupViews();
        return this.mContentView;
    }

    @Override
    public void onDestroyView() {
        if (!this.isOnline) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(this.mSongDeletedListener);
        }
        super.onDestroyView();
    }

    private class FragPagerAdapter extends FragmentPagerAdapter {
        private final Context mContext;

        @Override
        public int getItemPosition(Object object) {
            return -2;
        }

        public FragPagerAdapter(Context context, FragmentManager fm) {
            super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.mContext = context;
        }

        @Override
        public Fragment getItem(int pos) {
            return PagerFragment.this.getItem(pos);
        }
        @Override
        public CharSequence getPageTitle(int pos) {
            Common.ContainerType containerType = PagerFragment.this.mPageTypeList.get(pos);

            int stringId;

            if (containerType == Common.ContainerType.FOLDER_MODE) {
                stringId = PagerFragment.this.isOnline
                        ? R.string.category_folder
                        : R.string.all_songs;
            } else {
                stringId = containerType.getStringId();
            }

            if (stringId != 0) {
                String string = this.mContext.getString(stringId);

                if (PagerFragment.this.mParent == PagerParent.SEARCH_ACTIVITY
                        && PagerFragment.this.mPageTypeSizeMap.containsKey(containerType)) {

                    return string + " (" +
                            PagerFragment.this.mPageTypeSizeMap.get(containerType) +
                            ")";
                }

                return string;
            }

            return "";
        }

        @Override
        public int getCount() {
            return PagerFragment.this.mPageTypeList.size();
        }
    }

    public ContentFragment getItem(int position) {
        ContentFragment contentFragment = this.mFragmentLists[position];
        if (contentFragment != null) {
            return contentFragment;
        }
        ContentFragment contentFragmentNewInstance = ContentFragment.Companion.newInstance(this.isOnline, this.mPageTypeList.get(position), this.mFilterKey, this, this.mCurrentPage == position || this.mParent == PagerParent.SEARCH_ACTIVITY);
        contentFragmentNewInstance.setMenuVisibility(true);
        this.mFragmentLists[position] = contentFragmentNewInstance;
        return contentFragmentNewInstance;
    }

    private static Common.ContainerType[] getTitleIndex(PagerParent parent) {

        if (parent == PagerParent.SEARCH_ACTIVITY) {

            if (ConnectionManager.isUseWebAPI() || !Common.isLogin()) {
                return search_title_value;
            } else {
                return cgi_search_title_value;
            }
        }

        return main_title_value;
    }

    private List<Common.ContainerType> getAvailablePageList() {
        ArrayList<Common.ContainerType> arrayList = new ArrayList<>();
        for (Common.ContainerType containerType : getTitleIndex(this.mParent)) {
            if (!this.isOnline || ((!containerType.equals(Common.ContainerType.RADIO_MODE) || (Common.isLogin() && Common.haveInternetRadio())) && (!containerType.equals(Common.ContainerType.COMPOSER_MODE) || (Common.isLogin() && Common.supportComposer())))) {
                arrayList.add(containerType);
            }
        }
        return arrayList;
    }

    private void setupViews() throws Resources.NotFoundException {
        int size = this.mPageTypeList.size();
        this.mFragmentLists = new ContentFragment[size];
        this.BundleStacks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.BundleStacks.add(new Stack<>());
        }
        this.mPagerAdapter = new FragPagerAdapter(getActivity(), getChildFragmentManager());
        ViewPager viewPager = this.mContentView.findViewById(R.id.pager);
        this.mViewPager = viewPager;
        viewPager.setAdapter(this.mPagerAdapter);
        this.mViewPager.setOffscreenPageLimit(this.mPageTypeList.size());
        if (getActivity() instanceof HomeActivity) {
            // ((MainActivity) getActivity()).getTab();
            this.mTab = getActivity().findViewById(R.id.tab);
        } else {
            SynoLog.d(LOG, "Not HomeActivity");
//            this.mTab = ((SearchActivity) getActivity()).getTab();
        }
        if (this.mTab != null) {
            if (StateManager.getInstance().isMobile() && this.mFragmentLists.length > 3) {
                this.mTab.setTabMode(TabLayout.MODE_SCROLLABLE);
            }
            this.mTab.setVisibility(View.VISIBLE);
            this.mTab.clearOnTabSelectedListeners();
            this.mTab.setupWithViewPager(this.mViewPager);
            this.mTab.addOnTabSelectedListener(this.mOnTabSelectedListener);
            if (2 > size) {
                this.mTab.setVisibility(View.GONE);
            }
        }
        this.mViewPager.setCurrentItem(this.mCurrentPage, false);
    }

    private void determinePageList() {
        Common.ContainerType localPage;
        this.mPageTypeList.addAll(getAvailablePageList());
        if (this.isOnline) {
            localPage = AudioPreference.getLibraryPage(requireContext());
        } else {
            localPage = AudioPreference.getLocalPage(requireContext());
        }
        this.mCurrentPage = 0;
        Iterator<Common.ContainerType> it = this.mPageTypeList.iterator();
        while (it.hasNext() && !it.next().equals(localPage)) {
            this.mCurrentPage++;
        }
        if (this.mCurrentPage >= this.mPageTypeList.size()) {
            this.mCurrentPage = 0;
        }
    }

    @Override
    public void onAttach(Context activity) {
        SynoLog.d(LOG, this.isOnline + " : onAttach");
        super.onAttach(activity);
        ActivityResultCaller parentFragment = getParentFragment();
        if (parentFragment instanceof Callbacks) {
            this.mOnUpdateTitle = (Callbacks) parentFragment;
        } else if (activity instanceof Callbacks) {
            this.mOnUpdateTitle = (Callbacks) activity;
        }
        this.mDisposableOnViewTypeChanged = this.mSubjectViewTypeChanged.debounce(200L, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer() {
            @Override
            public void accept(Object obj) {
                AudioPreference.setViewMode(AudioPreference.getViewMode().toggle());
                for (ContentFragment contentFragment : mFragmentLists) {
                    if (contentFragment != null) {
                        contentFragment.toggleView();
                    }
                }
            }
        });
    }

    @Override
    public void onDetach() {
        SynoLog.d(LOG, this.isOnline + " : onDetach");
        super.onDetach();
        this.mOnUpdateTitle = sDummyUpdateTitle;
        Disposable disposable = this.mDisposableOnViewTypeChanged;
        if (disposable == null || disposable.isDisposed()) {
            return;
        }
        this.mDisposableOnViewTypeChanged.dispose();
        this.mDisposableOnViewTypeChanged = null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SynoLog.d(LOG, this.isOnline + " onPrepareOptionsMenu");
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr != null && contentFragmentArr[this.mCurrentPage] != null) {
            MenuItem menuItemFindItem = menu.findItem(R.id.menu_view);
            MenuItem menuItemFindItem2 = menu.findItem(R.id.menu_edit);
            MenuItem menuItemFindItem3 = menu.findItem(R.id.menu_play_all);
            MenuItem menuItemFindItem4 = menu.findItem(R.id.menu_add_all);
            if (menuItemFindItem != null) {
                menuItemFindItem.setVisible(this.mFragmentLists[this.mCurrentPage].canSetView());
            }
            if (menuItemFindItem2 != null) {
                menuItemFindItem2.setVisible(this.mFragmentLists[this.mCurrentPage].canMultiEdit());
            }
            boolean zIsPlayable = (Common.isLogin() || Common.isPlayModeStreaming()) & this.mFragmentLists[this.mCurrentPage].isPlayable();
            menuItemFindItem3.setVisible(zIsPlayable);
            menuItemFindItem4.setVisible(zIsPlayable);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SynoLog.d(LOG, this.isOnline + " onOptionsItemSelected : " + item.getTitle());
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr == null || contentFragmentArr[this.mCurrentPage] == null || item.getItemId() != R.id.menu_view) {
            return false;
        }
        this.mSubjectViewTypeChanged.onNext(true);
        return true;
    }

    @Override
    public void onContainerItemClick(Bundle bundle) {
        SynoLog.d(LOG, "onContainerItemClick: " + bundle.toString());
        this.currentPageBundle = bundle;
        SynoLog.d(LOG, "onClick : " + bundle);
        this.BundleStacks.get(this.mCurrentPage).push(this.mFragmentLists[this.mCurrentPage].getInitialBundle());
        FragmentManager childFragmentManager = getChildFragmentManager();
        childFragmentManager.beginTransaction().remove(this.mFragmentLists[this.mCurrentPage]).commit();
        childFragmentManager.executePendingTransactions();
        this.mFragmentLists[this.mCurrentPage] = ContentFragment.Companion.newInstance(bundle, this);
        this.mPagerAdapter.notifyDataSetChanged();
    }

    public String getTitle() {
        ContentFragment contentFragment;
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr == null || (contentFragment = contentFragmentArr[this.mCurrentPage]) == null) {
            return null;
        }
        return contentFragment.getTitle();
    }

    public boolean isEditable() {
        ContentFragment contentFragment;
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr == null || (contentFragment = contentFragmentArr[this.mCurrentPage]) == null) {
            return false;
        }
        return contentFragment.canMultiEdit();
    }

    public boolean canSetView() {
        ContentFragment contentFragment;
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr == null || (contentFragment = contentFragmentArr[this.mCurrentPage]) == null) {
            return false;
        }
        return contentFragment.canSetView();
    }

    public boolean hasBackStack() {
        ArrayList<Stack<Bundle>> arrayList = this.BundleStacks;
        return arrayList != null && !arrayList.get(this.mCurrentPage).isEmpty();
    }

    public boolean handleBack() {
        ContentFragment contentFragment;
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr != null && (contentFragment = contentFragmentArr[this.mCurrentPage]) != null) {
            if (contentFragment.isEditMode()) {
                this.mFragmentLists[this.mCurrentPage].setEditMode(false);
                return true;
            }
            if (hasBackStack()) {
                FragmentManager childFragmentManager = getChildFragmentManager();
                childFragmentManager.beginTransaction().remove(this.mFragmentLists[this.mCurrentPage]).commit();
                childFragmentManager.executePendingTransactions();
                ContentFragment[] contentFragmentArr2 = this.mFragmentLists;
                int i = this.mCurrentPage;
                contentFragmentArr2[i] = ContentFragment.Companion.newInstance(this.BundleStacks.get(i).pop(), this);
                this.mPagerAdapter.notifyDataSetChanged();
                this.mOnUpdateTitle.doUpdateTitle();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onUpdateTitle() {
        this.mOnUpdateTitle.doUpdateTitle();
    }

    @Override
    public void onFinishLoading(@NonNull Common.ContainerType type, int size) {
        if (this.mParent == PagerParent.SEARCH_ACTIVITY) {
            if (type == Common.ContainerType.SEARCH_ALBUM_MODE || type == Common.ContainerType.SEARCH_ARTIST_MODE || type == Common.ContainerType.SEARCH_SONG_MODE) {
                this.mPageTypeSizeMap.put(type, size);
            }
            TabLayout tabLayout = this.mTab;
            if (tabLayout != null) {
                tabLayout.setupWithViewPager(this.mViewPager);
            }
        }
    }

    @Override
    public Stack<Bundle> getBundleStack() {
        Stack<Bundle> stack = new Stack<>();
        stack.addAll(this.BundleStacks.get(this.mCurrentPage));
        stack.push(this.currentPageBundle);
        return stack;
    }
}