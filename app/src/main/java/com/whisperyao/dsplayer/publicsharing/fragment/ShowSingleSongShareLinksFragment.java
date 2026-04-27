package com.whisperyao.dsplayer.publicsharing.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCaller;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.webkit.internal.AssetHelper;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.vos.BaseSongSharingResponseVo;


public class ShowSingleSongShareLinksFragment extends DialogFragment {
    private static final String EXTRA_SONG = "song";
    private Callbacks mCallbacks;
    private TextView mShareButton;
    private SongItem mSong;
    private Switch mSwitchEnableSharing;
    private TextView mTextViewUrl;
    private boolean mHasChangedSharing = false;
    private BaseSongSharingResponseVo mSongSharing = BaseSongSharingResponseVo.DefaultBaseSongSharingResponseVo;
    private AsyncTask mGetSongTask = null;
    private ProgressDialog mGetSongDialog = null;
    private boolean mIsGetingSong = false;

    public interface Callbacks {
        void onShared(SongItem song);
    }

    public static ShowSingleSongShareLinksFragment newInstance(SongItem song) {
        ShowSingleSongShareLinksFragment showSingleSongShareLinksFragment = new ShowSingleSongShareLinksFragment();
        Bundle bundle = new Bundle();
        bundle.putBundle("song", song.getBundle());
        showSingleSongShareLinksFragment.setArguments(bundle);
        return showSingleSongShareLinksFragment;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ActivityResultCaller parentFragment = getParentFragment();
        if (parentFragment instanceof Callbacks) {
            this.mCallbacks = (Callbacks) parentFragment;
        } else if (activity instanceof Callbacks) {
            this.mCallbacks = (Callbacks) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mCallbacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(1, 2132017887);
        this.mSong = SongItem.fromBundle(getArguments().getBundle("song"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_song_share_links, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        getSongSharing();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mIsGetingSong) {
            this.mGetSongDialog.show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.mHasChangedSharing) {
            notifyOnShareSong(this.mSong);
        }
    }

    private void setupViews(View rootView) {
        ((Toolbar) rootView.findViewById(R.id.toolbar)).setTitle(R.string.sharing_shared_link);
        this.mTextViewUrl = rootView.findViewById(R.id.share_links);
        Switch r0 = rootView.findViewById(R.id.toggle_enable_sharing);
        this.mSwitchEnableSharing = r0;
        r0.setOnCheckedChangeListener((buttonView, isChecked) -> ShowSingleSongShareLinksFragment.this.setSongSharing(isChecked));
        TextView textView = rootView.findViewById(R.id.btn_done);
        this.mShareButton = textView;
        textView.setText(R.string.sharing_share);
        this.mShareButton.setOnClickListener(v -> ShowSingleSongShareLinksFragment.this.sendShareIntent());
        TextView textView2 = rootView.findViewById(R.id.btn_cancel);
        textView2.setText(R.string.str_done);
        textView2.setOnClickListener(v -> ShowSingleSongShareLinksFragment.this.dismiss());
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        this.mGetSongDialog = progressDialog;
        progressDialog.setMessage(getString(R.string.loading));
        this.mGetSongDialog.setCanceledOnTouchOutside(false);
        this.mGetSongDialog.setOnCancelListener(dialog -> {
            if (ShowSingleSongShareLinksFragment.this.mGetSongTask != null) {
                ShowSingleSongShareLinksFragment.this.mGetSongTask.cancel(true);
            }
            ShowSingleSongShareLinksFragment.this.mIsGetingSong = false;
        });
        updateSongSharing(BaseSongSharingResponseVo.DefaultBaseSongSharingResponseVo);
    }

    private void getSongSharing() {
        this.mGetSongTask = new AsyncTask<Void, Void, BaseSongSharingResponseVo>() {
            @Override
            protected void onPreExecute() {
                ShowSingleSongShareLinksFragment.this.mIsGetingSong = true;
            }

            @Override
            protected BaseSongSharingResponseVo doInBackground(Void... params) {
                return new BaseSongSharingResponseVo() {
                    @Override
                    public String getUrl() {
                        return "";
                    }

                    @Override
                    public boolean isEnabled() {
                        return false;
                    }
                };
//                return CacheManager.getInstance().doGetSongSharing(ShowSingleSongShareLinksFragment.this.mSong);
            }

            @Override
            protected void onPostExecute(BaseSongSharingResponseVo result) {
                ShowSingleSongShareLinksFragment.this.updateSongSharing(result);
                if (ShowSingleSongShareLinksFragment.this.mGetSongDialog != null) {
                    ShowSingleSongShareLinksFragment.this.mGetSongDialog.dismiss();
                }
                ShowSingleSongShareLinksFragment.this.mIsGetingSong = false;
            }
        }.execute();
    }

    protected void setSongSharing(final boolean toEnable) {
        new AsyncTask<Void, Void, BaseSongSharingResponseVo>() {
            ProgressDialog mDialog;

            @Override
            protected void onPreExecute() {
                ProgressDialog progressDialog = new ProgressDialog(ShowSingleSongShareLinksFragment.this.getActivity());
                this.mDialog = progressDialog;
                progressDialog.setMessage(ShowSingleSongShareLinksFragment.this.getString(R.string.loading));
                this.mDialog.setCanceledOnTouchOutside(false);
                this.mDialog.setOnCancelListener(dialog -> cancel(true));
                this.mDialog.show();
            }

            @Override
            protected BaseSongSharingResponseVo doInBackground(Void... params) {
                return new BaseSongSharingResponseVo() {
                    @Override
                    public String getUrl() {
                        return "";
                    }

                    @Override
                    public boolean isEnabled() {
                        return false;
                    }
                };
//                return CacheManager.getInstance().doSetSongSharing(ShowSingleSongShareLinksFragment.this.mSong, toEnable);
            }

            @Override
            protected void onPostExecute(BaseSongSharingResponseVo result) {
                ShowSingleSongShareLinksFragment.this.updateSongSharing(result);
                this.mDialog.dismiss();
                ShowSingleSongShareLinksFragment.this.mHasChangedSharing = true;
            }
        }.execute();
    }

    private void updateSongSharing(BaseSongSharingResponseVo songSharing) {
        if (songSharing != null) {
            boolean zIsEnabled = songSharing.isEnabled();
            this.mSwitchEnableSharing.setChecked(zIsEnabled);
            this.mTextViewUrl.setEnabled(zIsEnabled);
            this.mTextViewUrl.setText(songSharing.getUrl());
            this.mShareButton.setEnabled(zIsEnabled);
            this.mSongSharing = songSharing;
        }
    }

    private void sendShareIntent() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.TEXT", this.mSongSharing.getUrl());
        intent.setType(AssetHelper.DEFAULT_MIME_TYPE);
        startActivity(Intent.createChooser(intent, getString(R.string.sharing_share)));
    }

    private void notifyOnShareSong(SongItem song) {
        Callbacks callbacks = this.mCallbacks;
        if (callbacks != null) {
            callbacks.onShared(song);
        }
    }
}
