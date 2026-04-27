package com.whisperyao.dsplayer;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.whisperyao.dsplayer.databinding.PlayerChooserBinding;
import com.whisperyao.dsplayer.mediasession.client.MediaBrowserHelper;
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService;
import com.whisperyao.dsplayer.model.data.PlayingQueueManager;
import com.whisperyao.dsplayer.playing.Player;
import com.whisperyao.dsplayer.playing.PlayingStatusManager;
//import com.whisperyao.dsplayer.util.firebase.FirebaseAnalyticsUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;

public class PlayerChooserActivity extends TestRendererActivity {
    private static final String LOG = "PlayerChooserActivity";

    @Inject
    Provider<Class<? extends AbstractMediaBrowserService>> classProvider;
    private Button mButtonBack;
    private View mButtonLayout;

    private GroupPlayerEditorAdapter mGroupPlayerEditorAdapter;
    private ListView mListViewGroup;
    private ListView mListViewSingle;
    private MediaBrowserConnection mMediaBrowserHelper;
    private BaseAdapter mPlayerAdapter;

    @Inject
    PlayingStatusManager mPlayerStatusManager;
    private int mPositionTestPassword;
    private View mProgress;
    private ProgressDialog mProgressDialog;
    private Player mSelectedPlayer;

    @Inject
    PlayingQueueManager playingQueueManager;

    @Inject
    PlayingStatusManager playingStatusManager;
    private Mode mMode = Mode.single;
    private PlayingStatusManager.PlayerSetObserver mPlayerSetObserver = new PlayingStatusManager.PlayerSetObserver() {
        @Override
        public void onPlayerChange(Player player) {
            PlayerChooserActivity.this.mSelectedPlayer = player;
            PlayerChooserActivity.this.selectPlayer();
        }

        @Override
        public void onPlayerSetChanged() {
            PlayerChooserActivity.this.runOnUiThread((Runnable) () -> onPlayerSetChangedOnUiThread());
        }

        private void onPlayerSetChangedOnUiThread() {
            if (PlayerChooserActivity.this.mPlayerAdapter != null) {
                PlayerChooserActivity.this.mPlayerAdapter.notifyDataSetChanged();
                PlayerChooserActivity.this.selectPlayer();
            }
        }
    };
    private boolean isInitialConnection = true;

    private enum Mode {
        single,
        group
    }


    @Override
    public void onCreate(final Bundle state) {
        super.onCreate(state);
        PlayerChooserBinding playerChooserBindingInflate = PlayerChooserBinding.inflate(getLayoutInflater());
        setContentView(playerChooserBindingInflate.getRoot());
        initView(playerChooserBindingInflate);
        setTitle(R.string.select_player);
        this.mListViewSingle.setOnItemClickListener((adapterView, view, i, j) -> {
            mSelectedPlayer = mPlayerStatusManager.getPlayers().get(i);
            selectPlayer();
        });
        this.mListViewGroup.setOnItemClickListener((adapterView, view, i, j) -> {
            if (mListViewGroup.getCheckedItemPositions().get(i, false)) {
                Player item = mGroupPlayerEditorAdapter.getItem(i);
                if (item.getHasPassword() && ConnectionManager.isUseWebAPI()) {
                    mPositionTestPassword = i;
                    testPassword(item);
                }
            }
        });
        startMediaBrowserConnection();
        this.mSelectedPlayer = this.mPlayerStatusManager.getPlayer();
        SinglePlayerChooserAdapter singlePlayerChooserAdapter = new SinglePlayerChooserAdapter(this, this.mPlayerStatusManager);
        this.mPlayerAdapter = singlePlayerChooserAdapter;
        this.mListViewSingle.setAdapter(singlePlayerChooserAdapter);
        this.mPlayerStatusManager.registerPlayerSetChanged(this.mPlayerSetObserver);
        loadPlayers();
    }

    private void loadPlayers() {
        this.mPlayerStatusManager.requestLoadPlayers(new PlayingStatusManager.LoadPlayerCallback() { // from class: com.synology.dsaudio.PlayerChooserActivity.2
            @Override
            public void onPreLoad() {
                PlayerChooserActivity.this.switchToLoading();
            }

            @Override
            public void onPostLoad() {
                if (PlayerChooserActivity.this.isFinishing()) {
                    return;
                }
                PlayerChooserActivity.this.switchToSinglePlayerChooser();
                PlayerChooserActivity.this.selectPlayer();
            }
        });
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        MediaBrowserConnection mediaBrowserConnection = this.mMediaBrowserHelper;
        if (mediaBrowserConnection != null) {
            mediaBrowserConnection.onStop();
            this.mMediaBrowserHelper = null;
        }
        this.mPlayerStatusManager.unregisterPlayerSetChanged(this.mPlayerSetObserver);
        super.onDestroy();
    }

    private void initView(PlayerChooserBinding binding) {
        this.mListViewSingle = binding.singlePlayerChooser;
        this.mListViewGroup = binding.groupPlayerEditor;
        this.mProgress = binding.playerChooserProgress.getRoot();
        this.mButtonLayout = binding.PlayerChooserButtonLayout;
        binding.PlayerChooserBtnOK.setOnClickListener(view -> {
            if (mMode.equals(Mode.single)) {
                finishChoosePlayer();
            } else {
                finishEditGroupPlayer();
            }
        });
        Button button = binding.PlayerChooserBtnBack;
        this.mButtonBack = button;
        button.setOnClickListener(view -> performClickBack());
    }

    private void switchToLoading() {
        this.mProgress.setVisibility(0);
        this.mListViewSingle.setVisibility(8);
        this.mListViewGroup.setVisibility(8);
        this.mButtonLayout.setVisibility(8);
    }

    private void switchToSinglePlayerChooser() {
        setTitle(R.string.select_player);
        this.mProgress.setVisibility(8);
        this.mListViewSingle.setVisibility(0);
        this.mListViewGroup.setVisibility(8);
        this.mButtonBack.setVisibility(8);
        this.mButtonLayout.setVisibility(0);
        this.mMode = Mode.single;
    }

    private void switchToGroupPlayerEditor(final Player player) {
        setTitle(R.string.select_airplay_devices);
        this.mProgress.setVisibility(8);
        this.mListViewSingle.setVisibility(8);
        this.mListViewGroup.setVisibility(0);
        this.mButtonBack.setVisibility(0);
        this.mButtonLayout.setVisibility(0);
        GroupPlayerEditorAdapter groupPlayerEditorAdapter = new GroupPlayerEditorAdapter(this, player);
        this.mGroupPlayerEditorAdapter = groupPlayerEditorAdapter;
        this.mListViewGroup.setAdapter((ListAdapter) groupPlayerEditorAdapter);
        for (int i = 0; i < this.mGroupPlayerEditorAdapter.getCount(); i++) {
            this.mListViewGroup.setItemChecked(i, this.mGroupPlayerEditorAdapter.hasSelected(this.mGroupPlayerEditorAdapter.getItem(i)));
        }
        this.mMode = Mode.group;
    }

    private void finishChoosePlayer() {
        int checkedItemPosition = this.mListViewSingle.getCheckedItemPosition();
        List<Player> players = this.mPlayerStatusManager.getPlayers();
        if (checkedItemPosition < 0 || checkedItemPosition >= players.size()) {
            checkedItemPosition = 0;
        }
        Player player = players.get(checkedItemPosition);
        if (!this.playingStatusManager.isCurrentPlayer(player)) {
            if (player.getHasPassword() && ConnectionManager.isUseWebAPI()) {
                testPassword(player);
                return;
            } else {
                doChangePlayer(player);
                doLog();
                return;
            }
        }
        finishAndSetResultOK();
    }

    private void doLog() {
        String str;
        if (this.mSelectedPlayer.isRemotePlayer()) {
            if (this.mSelectedPlayer.getPlayerType() == Player.PlayerType.AIRPLAY) {
                str = "airplay";
            } else if (this.mSelectedPlayer.getPlayerType() == Player.PlayerType.CHROMECAST) {
                str = "chromecast";
            } else if (this.mSelectedPlayer.getPlayerType() == Player.PlayerType.USB) {
                str = "usb";
            } else {
                str = this.mSelectedPlayer.getPlayerType() != Player.PlayerType.UPNP ? null : "dlna";
            }
            if (str != null) {
                Bundle bundle = new Bundle();
                bundle.putString(UDCEvent.KEY_DEVICE, str);
//                this.mFirebaseAnalyticsUtil.logEvent(UDCEvent.EVENT__REMOTE_PLAYER, bundle);
            }
        }
    }


    private void doChangePlayer(Player player) {
        this.mPlayerStatusManager.setPlayer(player);
        this.mMediaBrowserHelper.getTransportControls().sendCustomAction(AbstractMediaBrowserService.CUSTOM_ACTION_SWITCH_PLAYER, (Bundle) null);
        Common.gModeSwitchMode = true;
        Common.gDeviceChanged = false;
        bindService();
    }

    private void finishAndSetResultOK() {
        ProgressDialog progressDialog = this.mProgressDialog;
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        setResult(-1);
        finish();
    }

    private void startMediaBrowserConnection() {
        MediaBrowserConnection mediaBrowserConnection = this.mMediaBrowserHelper;
        if (mediaBrowserConnection != null) {
            mediaBrowserConnection.onStop();
        }
        MediaBrowserConnection mediaBrowserConnection2 = new MediaBrowserConnection(this);
        this.mMediaBrowserHelper = mediaBrowserConnection2;
        mediaBrowserConnection2.onStart();
    }

    private class MediaBrowserConnection extends MediaBrowserHelper {
        public MediaBrowserConnection(Context mContext) {
            super(mContext, PlayerChooserActivity.this.mPlayerStatusManager, PlayerChooserActivity.this.playingQueueManager,
                    PlayerChooserActivity.this.classProvider.get());
        }

        @Override
        protected void onConnected(MediaControllerCompat mediaController) {
            if (PlayerChooserActivity.this.isInitialConnection) {
                PlayerChooserActivity.this.isInitialConnection = false;
            } else {
                PlayerChooserActivity.this.mPlayerStatusManager.switchPlayer.onNext(true);
                PlayerChooserActivity.this.finishAndSetResultOK();
            }
        }
    }

    private void bindService() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        this.mProgressDialog = progressDialog;
        progressDialog.setCancelable(false);
        this.mProgressDialog.setCanceledOnTouchOutside(false);
        this.mProgressDialog.setMessage(getString(R.string.processing));
        this.mProgressDialog.show();
        startMediaBrowserConnection();
    }

    private void finishEditGroupPlayer() {
        SparseBooleanArray checkedItemPositions = this.mListViewGroup.getCheckedItemPositions();
        final Player player = this.mGroupPlayerEditorAdapter.getPlayer();
        final ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mGroupPlayerEditorAdapter.getCount(); i++) {
            Player item = this.mGroupPlayerEditorAdapter.getItem(i);
            if (checkedItemPositions.get(i, false)) {
                arrayList.add(item.getUniqueId());
            }
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> new AsyncTask<Void, Void, Void>() {
            @Override // android.os.AsyncTask
            protected void onPreExecute() {
                PlayerChooserActivity.this.switchToLoading();
            }

            @Override // android.os.AsyncTask
            protected Void doInBackground(Void... params) {
                RemoteController.setGroupPlayer(player.getUniqueId(), arrayList);
                return null;
            }

            @Override // android.os.AsyncTask
            protected void onPostExecute(Void result) {
                PlayerChooserActivity.this.performClickBack();
                PlayerChooserActivity.this.loadPlayers();
            }

            @Override // android.os.AsyncTask
            protected void onCancelled() {
                PlayerChooserActivity.this.performClickBack();
                PlayerChooserActivity.this.loadPlayers();
            }
        }.execute(new Void[0]), 300L);
    }


    void performClickBack() {
        onBackPressed();
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        if (this.mMode.equals(Mode.single)) {
            finish();
        } else {
            switchToSinglePlayerChooser();
        }
    }

    private synchronized void selectPlayer() {
        this.mListViewSingle.setItemChecked(this.mPlayerStatusManager.getPlayers().indexOf(this.mSelectedPlayer), true);
    }

    @Override // com.synology.dsaudio.TestRendererActivity
    protected void passSettingRemotePlayerPassword(Player player) {
        if (this.mMode.equals(Mode.single)) {
            doChangePlayer(player);
        }
    }

    @Override // com.synology.dsaudio.TestRendererActivity
    protected void cancelSettingRemotePlayerPassword() {
        if (this.mMode.equals(Mode.single)) {
            this.mListViewSingle.setItemChecked(this.mPlayerStatusManager.getPlayers().indexOf(this.mSelectedPlayer), true);
        } else {
            this.mListViewGroup.setItemChecked(this.mPositionTestPassword, false);
        }
    }

    private class SinglePlayerChooserAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private PlayingStatusManager mPlayerStatusManager;

        @Override
        public long getItemId(int position) {
            return position;
        }

        SinglePlayerChooserAdapter(Context context, PlayingStatusManager playerStatusManager) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mPlayerStatusManager = playerStatusManager;
        }

        @Override
        public int getCount() {
            return this.mPlayerStatusManager.getPlayers().size();
        }

        @Override
        public Player getItem(int position) {
            if (position < 0 || position >= this.mPlayerStatusManager.getPlayers().size()) {
                return null;
            }
            return this.mPlayerStatusManager.getPlayers().get(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.player_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.mNameTextView = convertView.findViewById(R.id.player_name);
                viewHolder.mLockImageView = convertView.findViewById(R.id.player_lock);
                viewHolder.mInfoImageView = convertView.findViewById(R.id.player_info);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Player item = getItem(position);
            viewHolder.mNameTextView.setText(item.getName());
            viewHolder.mLockImageView.setVisibility(item.getHasPassword() ? 0 : 8);
            viewHolder.mInfoImageView.setVisibility(item.isGroupPlayer() ? 0 : 8);
            viewHolder.mInfoImageView.setOnClickListener(new View.OnClickListener() {
                private int mIndex;

                {
                    this.mIndex = position;
                }

                @Override
                public void onClick(View v) {
                    PlayerChooserActivity.this.switchToGroupPlayerEditor(SinglePlayerChooserAdapter.this.getItem(this.mIndex));
                }
            });
            return convertView;
        }

        private class ViewHolder {
            private ImageView mInfoImageView;
            private ImageView mLockImageView;
            private TextView mNameTextView;

            private ViewHolder() {
            }
        }
    }

    private class GroupPlayerEditorAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        Player mPlayer;
        List<Player> mSubPlayers = new ArrayList();

        @Override
        public long getItemId(int position) {
            return position;
        }

        GroupPlayerEditorAdapter(Context context, Player player) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mPlayer = player;
            for (Player player2 : PlayerChooserActivity.this.playingStatusManager.getPlayers()) {
                if (!player2.isGroupPlayer() && this.mPlayer.getPlayerType().equals(player2.getPlayerType())) {
                    this.mSubPlayers.add(player2);
                }
            }
        }

        private boolean hasSelected(Player player) {
            for (Player value : this.mPlayer.getSubPlayers()) {
                if (player.getUniqueId().equals(value.getUniqueId())) {
                    return true;
                }
            }
            return false;
        }

        public Player getPlayer() {
            return this.mPlayer;
        }

        @Override
        public int getCount() {
            return this.mSubPlayers.size();
        }

        @Override
        public Player getItem(int position) {
            return this.mSubPlayers.get(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.player_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.mNameTextView = convertView.findViewById(R.id.player_name);
                viewHolder.mLockImageView = convertView.findViewById(R.id.player_lock);
                viewHolder.mInfoImageView = convertView.findViewById(R.id.player_info);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Player item = getItem(position);
            viewHolder.mNameTextView.setText(item.getName());
            viewHolder.mLockImageView.setVisibility(item.getHasPassword() ? View.VISIBLE : View.GONE);
            viewHolder.mInfoImageView.setVisibility(item.isGroupPlayer() ? View.VISIBLE : View.GONE);
            return convertView;
        }

        private class ViewHolder {
            private ImageView mInfoImageView;
            private ImageView mLockImageView;
            private CheckedTextView mNameTextView;

            private ViewHolder() {
            }
        }
    }
}
