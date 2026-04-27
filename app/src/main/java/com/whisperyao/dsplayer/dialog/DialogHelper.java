package com.whisperyao.dsplayer.dialog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import androidx.fragment.app.FragmentManager;
import com.whisperyao.dsplayer.R;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class DialogHelper {

    private static final String TAG = "DialogHelper";
    private static final String KEY_ID = "id";

    /**
     * 显示“添加到播放列表”选项
     */
    public static void listPlaylistOption(
            final Context context,
            final FragmentManager fragmentManager,
            final List<SongItem> items
    ) {
        String saveToPersonal = context.getString(R.string.save_to_personal);
        String saveToShared = context.getString(R.string.save_to_shared);
        String createPlaylist = context.getString(R.string.create_playlist);

        List<String> actions = new ArrayList<>();

        if (Common.editPersonalPlaylist()) {
            actions.add(saveToPersonal);
        }
        if (Common.editSharedPlaylist()) {
            actions.add(saveToShared);
        }
        if (Common.createSharedPlaylist() || Common.createPersonalPlaylist()) {
            actions.add(createPlaylist);
        }

        if (actions.isEmpty()) return;

        final String[] actionArray = actions.toArray(new String[0]);

        new AlertDialog.Builder(context)
                .setTitle(R.string.add_to_playlist)
                .setItems(actionArray, (dialog, which) -> {

                    SynoLog.d(TAG, "addToPlaylist onClick : " + which);

                    if (which >= actionArray.length) return;

                    String selected = actionArray[which];

                    if (saveToPersonal.equals(selected)) {
//                        choosePlaylist(context, fragmentManager, items, false);
                    } else if (saveToShared.equals(selected)) {
//                        choosePlaylist(context, fragmentManager, items, true);
                    } else if (createPlaylist.equals(selected)) {
                        createPlaylist(fragmentManager, items, false);
                    }
                })
                .show();
    }

    /**
     * 创建播放列表
     */
    public static void createPlaylist(
            FragmentManager fragmentManager,
            List<SongItem> items,
            boolean shared
    ) {
        createPlaylist(fragmentManager, items, shared, false);
    }

    public static void createPlaylist(
            FragmentManager fragmentManager,
            List<SongItem> items,
            boolean shared,
            boolean createByShare
    ) {
//        EditPlaylistFragment
//                .newInstance(EditPlaylistFragment.generateArgumentsForCreate(shared, items, createByShare))
//                .show(fragmentManager, "create_playlist");
    }

    /**
     * 选择播放列表
     */
    public static void choosePlaylist(
            final Context context,
            final FragmentManager fragmentManager,
            final List<SongItem> items,
            final boolean isShared
    ) {
        SynoLog.d(TAG, "choosePlaylist isShared : " + isShared);

        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.processing));

        ThreadWork work = new ThreadWork() {

            List<PlaylistItem> playlistList;
            boolean success = false;

            @Override
            public void preWork() {
                dialog.show();
            }

            @Override
            public void onWorking() {
//                playlistList = CacheManager.getInstance()
//                        .doEnumNormalPlaylist(isShared)
//                        .getItemList();
                success = true;
            }

            @Override
            public void onComplete() {
                dialog.dismiss();

                if (!success || playlistList.isEmpty()) {
                    createPlaylist(fragmentManager, items, isShared);
                    return;
                }

                CharSequence[] names = new CharSequence[playlistList.size()];
                for (int i = 0; i < playlistList.size(); i++) {
                    names[i] = playlistList.get(i).getTitle();
                }

                new AlertDialog.Builder(context)
                        .setTitle(R.string.add_to_playlist)
                        .setSingleChoiceItems(names, 0, (d, which) -> {
                            SynoLog.d(TAG, "choosePlaylist onClick : " + which);
                        })
                        .setPositiveButton(R.string.ok, (d, which) -> {
                            int pos = ((AlertDialog) d)
                                    .getListView()
                                    .getCheckedItemPosition();

//                            addToPlaylist(context, playlistList.get(pos), 0, items);
                        })
                        .show();
            }
        };

        dialog.setOnCancelListener(d -> work.endThread());
        work.startWork();
    }

    /**
     * 添加歌曲到播放列表
     */
    public static void addToPlaylist(
            final Context context,
            final PlaylistItem playlistItem,
            final int index,
            final List<SongItem> items
    ) {

        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.processing));

        ThreadWork work = new ThreadWork() {

            Common.ConnectionInfo info;
            boolean success = false;

            @Override
            public void preWork() {
                dialog.show();
            }

            @Override
            public void onWorking() {
//                String idList = Utilities.createIdList(items);
//                    SynoLog.d(TAG, "addToPlaylist idList = " + idList);

//                    info = PlaylistEditor.addToPlaylist(
//                            playlistItem.getID(),
//                            index,
//                            idList,
//                            null
//                    );

                success = true;

            }

            @Override
            public void onComplete() {
                dialog.dismiss();

                if (success && info.getResultVo() != null && info.getResultVo().getSuccess()) {

                    if (context != null) {
//                        String msg = context.getString(R.string.add_songs_to_playlist)
//                                .replace(Common.NUMBER, String.valueOf(items.size()))
//                                .replace(Common.PLAYLIST_NAME, playlistItem.getTitle());

//                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }

                    notifyPlaylistChanged(context, playlistItem);

                } else {
                    if (context != null) {
                        Toast.makeText(context, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        dialog.setOnCancelListener(d -> work.endThread());
        work.startWork();
    }

    /**
     * 通知播放列表变更（通过ID）
     */
    public static void notifyPlaylistChanged(Context context, String id) {
//        CacheManager.getInstance().clearPlaylistCache();

        Intent intent = new Intent(Common.ACTION_PLAYLIST_CHANGED);
        intent.putExtra(KEY_ID, id);

        if (context != null) {
            context.sendBroadcast(intent);
        }
    }

    /**
     * 通知播放列表变更（通过对象）
     */
    public static void notifyPlaylistChanged(Context context, PlaylistItem playlistItem) {
        String id = playlistItem.getID();

        CacheManager cache = CacheManager.getInstance();
//        cache.clearPlaylistCache();
//        cache.clearPlaylistSongCache(playlistItem);

        Intent intent = new Intent(Common.ACTION_PLAYLIST_CHANGED);
        intent.putExtra(KEY_ID, id);

        if (context != null) {
            context.sendBroadcast(intent);
        }
    }

    /**
     * 分享单曲
     */
    public static void shareSong(FragmentManager fragmentManager, SongItem song) {
//        ShowSingleSongShareLinksFragment
//                .newInstance(song)
//                .show(fragmentManager, null);
    }
}
