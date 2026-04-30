package com.whisperyao.dsplayer.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCaller;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class HomePageFragment extends Fragment implements ContentFragment.ContentCallback {
    private static final String LOG = "HomePageFragment";
    private static final Callbacks sDummyUpdateTitle = () -> { };
    private ArrayList<Stack<Bundle>> BundleStacks;
    private Bundle currentPageBundle;
    private View mContentView;
    private int mCurrentPage;
    private String mFilterKey;
    private ContentFragment[] mFragmentLists;
    private FragmentPagerAdapter mPagerAdapter;
    private TabLayout mTabs;
    private ViewPager mViewPager;
    private List<Common.ContainerType> mPageTypeList = new ArrayList<>();
    private Callbacks mOnUpdateTitle = sDummyUpdateTitle;

    public interface Callbacks {
        void doUpdateTitle();
    }

    @Override
    public void onFinishLoading(Common.ContainerType type, int size) { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SynoLog.d(LOG, " onCreate");
        determinePageList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SynoLog.d(LOG, " onCreateView");
        this.mContentView = inflater.inflate(R.layout.pager_fragment, null);
        setupViews();
        return this.mContentView;
    }


    @Override
    public void onAttach(Activity context) {
        SynoLog.d(LOG, " onAttach");
        super.onAttach(context);
        ActivityResultCaller parentFragment = getParentFragment();
        if (parentFragment instanceof Callbacks) {
            this.mOnUpdateTitle = (Callbacks) parentFragment;
        } else if (context instanceof Callbacks) {
            this.mOnUpdateTitle = (Callbacks) context;
        }
    }

    @Override
    public void onDetach() {
        SynoLog.d(LOG, " onDetach");
        super.onDetach();
        this.mOnUpdateTitle = sDummyUpdateTitle;
    }

    private void determinePageList() {
        this.mPageTypeList.clear();
        this.mPageTypeList.add(Common.ContainerType.HOMEPAGE_PIN_MODE);
        this.mPageTypeList.add(Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE);
        this.mPageTypeList.add(Common.ContainerType.HOMEPAGE_TEST_MODE);
        int iIndexOf = this.mPageTypeList.indexOf(AudioPreference.getHomePage());
        this.mCurrentPage = iIndexOf;
        if (iIndexOf < 0 || iIndexOf >= this.mPageTypeList.size()) {
            this.mCurrentPage = 0;
        }
    }

    private void setupViews() {
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
        TabLayout tab = getActivity().findViewById(R.id.tab);
        this.mTabs = tab;
        SynoLog.d(LOG, tab.toString());
        if (tab != null) {
            tab.setTabMode(TabLayout.MODE_FIXED);
            this.mTabs.setVisibility(View.VISIBLE);
            this.mTabs.clearOnTabSelectedListeners();
            this.mTabs.setupWithViewPager(this.mViewPager);
            this.mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabReselected(TabLayout.Tab tab2) { }

                @Override
                public void onTabUnselected(TabLayout.Tab tab2) { }

                @Override
                public void onTabSelected(TabLayout.Tab tab2) {
                    int position = tab2.getPosition();
                    SynoLog.d(HomePageFragment.LOG, "onPageSelected " + position);
                    if (HomePageFragment.this.mFragmentLists != null &&
                            HomePageFragment.this.mFragmentLists[HomePageFragment.this.mCurrentPage] != null &&
                            HomePageFragment.this.mFragmentLists[HomePageFragment.this.mCurrentPage].isEditMode()) {
                        HomePageFragment.this.mFragmentLists[HomePageFragment.this.mCurrentPage].setEditMode(false);
                    }
                    HomePageFragment.this.mCurrentPage = position;
                    if (HomePageFragment.this.mFragmentLists != null && HomePageFragment.this.mFragmentLists[HomePageFragment.this.mCurrentPage] != null) {
                        HomePageFragment.this.mFragmentLists[HomePageFragment.this.mCurrentPage].onPageSelected();
                    }
                    AudioPreference.setHomePage(HomePageFragment.this.mPageTypeList.get(HomePageFragment.this.mCurrentPage));
                    HomePageFragment.this.mOnUpdateTitle.doUpdateTitle();
                }
            });
            if (3 > size) {
                this.mTabs.setVisibility(View.GONE);
            }
        }
        this.mViewPager.setCurrentItem(this.mCurrentPage, false);
    }


    private ContentFragment getItem(int position) {
        SynoLog.i(LOG, " getItem : " + position);
        ContentFragment contentFragment = this.mFragmentLists[position];
        if (contentFragment != null) {
            return contentFragment;
        }
        ContentFragment contentFragmentNewInstance = ContentFragment.Companion.newInstance(true, this.mPageTypeList.get(position), this.mFilterKey, this, true);
        contentFragmentNewInstance.setMenuVisibility(true);
        this.mFragmentLists[position] = contentFragmentNewInstance;
        return contentFragmentNewInstance;
    }

    private class FragPagerAdapter extends FragmentPagerAdapter {
        private final Context mContext;

        @Override
        public int getItemPosition(Object object) {
            return -2;
        }

        FragPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.mContext = context;
        }

        @Override
        public Fragment getItem(int pos) {
            return HomePageFragment.this.getItem(pos);
        }

        @Override
        public CharSequence getPageTitle(int pos) {
            int stringId = HomePageFragment.this.mPageTypeList.get(pos).getStringId();
            if (stringId != 0) {
                return this.mContext.getString(stringId);
            }
            return "";
        }

        @Override
        public int getCount() {
            return HomePageFragment.this.mPageTypeList.size();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SynoLog.d(LOG, " onOptionsItemSelected : " + ((Object) item.getTitle()));
        ContentFragment[] contentFragmentArr = this.mFragmentLists;
        if (contentFragmentArr == null || contentFragmentArr[this.mCurrentPage] == null) {
            return false;
        }
        if (item.getItemId() == R.id.menu_view) {
            AudioPreference.setViewMode(AudioPreference.getViewMode().toggle());
            for (ContentFragment contentFragment : this.mFragmentLists) {
                if (contentFragment != null) {
                    contentFragment.toggleView();
                }
            }
            return true;
        }
        return this.mFragmentLists[this.mCurrentPage].onOptionsItemSelected(item);
    }

    @Override
    public void onContainerItemClick(Bundle bundle) {
        SynoLog.d(LOG, "onClick : " + bundle.toString());
        this.currentPageBundle = bundle;
        this.BundleStacks.get(this.mCurrentPage).push(this.mFragmentLists[this.mCurrentPage].getInitialBundle());
        FragmentManager childFragmentManager = getChildFragmentManager();
        childFragmentManager.beginTransaction().remove(this.mFragmentLists[this.mCurrentPage]).commit();
        childFragmentManager.executePendingTransactions();
        this.mFragmentLists[this.mCurrentPage] = ContentFragment.Companion.newInstance(bundle, this);
        this.mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onUpdateTitle() {
        this.mOnUpdateTitle.doUpdateTitle();
    }

    @Override
    public Stack<Bundle> getBundleStack() {
        Stack<Bundle> stack = new Stack<>();
        stack.addAll(this.BundleStacks.get(this.mCurrentPage));
        stack.push(this.currentPageBundle);
        return stack;
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
}
