package com.whisperyao.dsplayer.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.adapters.DrawerAdapter;
import com.whisperyao.dsplayer.item.DrawerItem;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import java.util.ArrayList;
import java.util.List;


public class DrawerFragment extends Fragment {
    private static final String CURRENT_ID = "currentId";
    private static final String LOG = "DrawerFragment";
    private Activity mActivity;
    private DrawerAdapter mAdapter;
    private NavigationDrawerCallbacks mCallbacks;
    private int mCurrentItemId;
    private ListView mDrawerList;

    public interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(int itemId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView listView = (ListView) inflater.inflate(R.layout.fragment_drawer_menu, container, false);
        this.mDrawerList = listView;
        listView.setOnItemClickListener(new DrawerItemClickListener());
        DrawerAdapter drawerAdapter = new DrawerAdapter(this.mActivity, createDrawerItems());
        this.mAdapter = drawerAdapter;
        this.mDrawerList.setAdapter(drawerAdapter);
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_ID)) {
            this.mCurrentItemId = savedInstanceState.getInt(CURRENT_ID);
            SynoLog.d(LOG, " onCreateView has CURRENT_ID = " + this.mCurrentItemId);
        }
        else if (!Common.isLogin()) {
            this.mCurrentItemId = 1;
        } else {
            this.mCurrentItemId = AudioPreference.getNavigationPref();
        }
        int i = this.mCurrentItemId;
        if (i <= 0 || i > 5) {
            if (ConnectionManager.hasHomepage()) {
                this.mCurrentItemId = 5;
            } else {
                this.mCurrentItemId = Common.isLogin() ? 2 : 1;
            }
        } else {
            if (i == 4 && !Common.haveInternetRadio()) {
                SynoLog.w(LOG, " invalid radio mCurrentItemId = " + this.mCurrentItemId);
                this.mCurrentItemId = 5;
            }
            if (this.mCurrentItemId == 5 && !ConnectionManager.hasHomepage()) {
                SynoLog.w(LOG, " invalid home mCurrentItemId = " + this.mCurrentItemId);
                this.mCurrentItemId = 2;
            }
        }
        SynoLog.d(LOG, " onCreateView mCurrentItemId = " + this.mCurrentItemId);
        return this.mDrawerList;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_ID, this.mCurrentItemId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mActivity = activity;
            this.mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException unused) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mCallbacks = null;
    }

    private List<DrawerItem> createDrawerItems() {
        ArrayList<DrawerItem> arrayList = new ArrayList<>();
        arrayList.add(DrawerItem.createSettingItem(R.drawable.nav_account, AudioPreference.getAccount(), AudioPreference.getIp(), 0));
        arrayList.add(DrawerItem.createNormalItem(R.drawable.nav_local, getString(R.string.local), 1));
        // ConnectionManager.hasHomepage()
        arrayList.add(DrawerItem.createNormalItem(R.drawable.nav_home, getString(R.string.category_homepage), 5));
        arrayList.add(DrawerItem.createNormalItem(R.drawable.nav_library, getString(R.string.music_library), 2));
        arrayList.add(DrawerItem.createNormalItem(R.drawable.nav_song_playlist, getString(R.string.category_playlist), 3));
        if (Common.haveInternetRadio()) {
            arrayList.add(DrawerItem.createNormalItem(R.drawable.nav_radio, getString(R.string.category_radio), 4));
        }
        arrayList.add(DrawerItem.createNormalItem(R.drawable.notification_icon, "SongListActivity", 6));
        return arrayList;
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DrawerFragment.this.onNavigationItemSelected(position);
        }
    }

    public void onNavigationItemSelected() {
        SynoLog.d("DrawerFragment","mCurrentId: " + this.mCurrentItemId + " " + this.mAdapter.getItemPosition(this.mCurrentItemId));

        if (this.mCurrentItemId == 1) {
            onNavigationItemSelected(6);
        }
        onNavigationItemSelected(this.mAdapter.getItemPosition(this.mCurrentItemId));
    }


    public void onNavigationItemSelected(final int itemPosition) {
        SynoLog.d(LOG, " onNavigationItemSelected , itemPosition = " + itemPosition);
        this.mAdapter.setSelectedItem(itemPosition);
        this.mDrawerList.setItemChecked(itemPosition, true);
        int itemId = ((DrawerItem) this.mAdapter.getItem(itemPosition)).getItemId();
        int i = this.mCurrentItemId;
        this.mCurrentItemId = itemId;
        this.mCallbacks.onNavigationDrawerItemSelected(itemId);
        if (itemId == 0) {
            this.mCurrentItemId = i;
            int itemPosition2 = this.mAdapter.getItemPosition(i);
            this.mAdapter.setSelectedItem(itemPosition2);
            this.mDrawerList.setItemChecked(itemPosition2, true);
        }
        this.mAdapter.notifyDataSetChanged();
    }
}