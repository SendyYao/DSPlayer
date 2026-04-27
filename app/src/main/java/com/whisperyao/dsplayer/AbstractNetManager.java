package com.whisperyao.dsplayer;

import android.os.Bundle;

import com.synology.sylib.util.NetworkUtils;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.vos.api.pin.PinListResponseVo;
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import okhttp3.Response;
import java.util.List;

public abstract class AbstractNetManager {

    public static final String NETWORK_UNREACHABLE = "Network unreachable";
    public static final String DS_IS_UNAVAILABLE = "DS is unavailable";
    protected static final String LIB_SHARED = "shared";

    public static String getPersonalLibraryValue() {
        Common.PrefPersonal personalPref = AudioPreference.getPersonalPref();
        if (personalPref.equals(Common.PrefPersonal.ALL)) {
            return "all";
        }
        if (personalPref.equals(Common.PrefPersonal.PERSONAL)) {
            return "personal";
        }
        return LIB_SHARED;
    }

    public abstract ConnectionManager.ResourceType getResourceType();

    protected abstract boolean isWithRating();

    public abstract boolean isOnline();

    protected boolean isSupportPin() {
        return false;
    }

    protected abstract boolean canSupportAddToNext();

    protected abstract boolean canSupportGenreArtist();

    protected abstract String getPlayUrl(SongItem song, boolean isForChromeCast);

    protected abstract List<SongItem> doSearch(final Common.SearchCategory category, final String key) throws JSONException, IOException;

    protected abstract void deleteRadioInfo(String stream_id);

    protected abstract JSONObject doPollingRadioInfo(String stream_id);

    protected abstract void doEnumLyrics(final String strFilename, final String id) throws IOException;

    protected PinListResponseVo doEnumPins() throws Exception {
        return null;
    }

    protected abstract void doEnumContainer(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException;

    protected abstract void doEnumContainerSongs(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException;

    protected abstract void doEnumFolderSongs(String key, boolean recursive, String cachePath, int pageNum) throws JSONException, IOException, NoSuchAlgorithmException, KeyManagementException;

    protected abstract BasePlaylistResponseVo doEnumPlaylist(int pageNum) throws Exception;
    protected abstract void doEnumRandom100(String cachePath) throws JSONException, IOException;

    protected abstract void doEnumRadio(String key, String cachePath, int pageNum) throws IOException;

    protected abstract void doEnumPlaylistSongs(PlaylistItem playlistItem, String cachePath, int pageNum) throws JSONException, IOException;

    protected void handleError(int status) throws IOException {
        if (NetworkUtils.isNetworkConnected(App.getContext())) {
            throw new IOException(NETWORK_UNREACHABLE);
        }
        throw new IOException(DS_IS_UNAVAILABLE);
    }

    public static void downloadStream(Response httpResponse, String cachePath) throws IOException {
        InputStream inputStreamByteStream = httpResponse.body().byteStream();
        File file = new File(cachePath);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(cachePath);
        byte[] bArr = new byte[1024];
        while (true) {
            int i = inputStreamByteStream.read(bArr);
            if (i > 0) {
                fileOutputStream.write(bArr, 0, i);
                fileOutputStream.flush();
            } else {
                fileOutputStream.close();
                inputStreamByteStream.close();
                httpResponse.close();
                return;
            }
        }
    }

    protected abstract int requestToGetSongRating(SongItem song) throws IOException;

}
