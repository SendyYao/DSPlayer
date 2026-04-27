package com.whisperyao.dsplayer;

import android.content.Context;
import android.os.RemoteException;
import com.whisperyao.dsplayer.chromecast.IChromeCastService;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.model.data.DataModelManager;
import com.whisperyao.dsplayer.playing.Player;
import com.whisperyao.dsplayer.playing.PlayingStatusManager;
import com.whisperyao.dsplayer.util.SynoLog;


public class ServiceOperator {
    public static final String BUFFERING_CHANGED = "com.synology.dsaudio.bufferingchanged";
    public static final String ERR_DEVICE_NOT_FOUND = "com.synology.dsaudio.ERR_DEVICE_NOT_FOUND";
    public static final String ERR_SESSION_ENDED = "com.synology.dsaudio.chromecast.SESSION_ENDED";
    private static final String LOG = "ServiceOperator";
    public static final String META_CHANGED = "com.synology.dsaudio.metachanged";
    public static boolean MainPageClosed = true;
    public static final String OPEN_UI = "com.synology.dsaudio.openui";
    public static final String PLAYSTATE_CHANGED = "com.synology.dsaudio.playstatechanged";
    public static final String PREPARE_CHANGED = "com.synology.dsaudio.preparechanged";
    public static boolean PlayerPageClosed = true;
    public static final String QUEUE_CHANGED = "com.synology.dsaudio.queuechanged";
    private static IChromeCastService gChromeCastService;
    private static IPlaybackService gPlaybackService;
    private static IRemoteControllerService gRemoteService;
    private static Player sPlayer = PlayingStatusManager.getLocalPlayer();

    public static void stopService(final Context context) {
    }

    public static boolean isUIClosed() {
        SynoLog.d(LOG, "isUIClosed : " + (MainPageClosed && PlayerPageClosed));
        return MainPageClosed && PlayerPageClosed;
    }

    public static void reloadAll() {
        IPlaybackService iPlaybackService;
        if (!sPlayer.isPlayModeStreaming() || (iPlaybackService = gPlaybackService) == null) {
            return;
        }
        try {
            iPlaybackService.reloadAll();
        } catch (RemoteException unused) {
        }
    }

    public static void savePauseStatus() {
        IPlaybackService iPlaybackService;
        if (!sPlayer.isPlayModeStreaming() || (iPlaybackService = gPlaybackService) == null) {
            return;
        }
        try {
            iPlaybackService.savePauseStatus();
        } catch (RemoteException unused) {
        }
    }

    public static boolean isPlaying() {
        IChromeCastService iChromeCastService;
        IRemoteControllerService iRemoteControllerService;
        IPlaybackService iPlaybackService;
        try {
            if (sPlayer.isPlayModeStreaming() && (iPlaybackService = gPlaybackService) != null) {
                return iPlaybackService.isPlaying() || gPlaybackService.isPreparing();
            }
            if (sPlayer.isPlayModeRenderer() && (iRemoteControllerService = gRemoteService) != null) {
                return iRemoteControllerService.isPlaying();
            }
            if (!sPlayer.isPlayModeChromeCast() || (iChromeCastService = gChromeCastService) == null) {
                return false;
            }
            return iChromeCastService.isPlaying();
        } catch (RemoteException unused) {
            return false;
        }
    }

    public static boolean isPlayingRadio() {
        IChromeCastService iChromeCastService;
        IRemoteControllerService iRemoteControllerService;
        IPlaybackService iPlaybackService;
        try {
            if (sPlayer.isPlayModeStreaming() && (iPlaybackService = gPlaybackService) != null) {
                return iPlaybackService.isPlayingRadio();
            }
            if (sPlayer.isPlayModeRenderer() && (iRemoteControllerService = gRemoteService) != null) {
                return iRemoteControllerService.isPlayingRadio();
            }
            if (!sPlayer.isPlayModeChromeCast() || (iChromeCastService = gChromeCastService) == null) {
                return false;
            }
            return iChromeCastService.isPlayingRadio();
        } catch (RemoteException unused) {
            return false;
        }
    }

    public static boolean isPreparing() {
        IChromeCastService iChromeCastService;
        IPlaybackService iPlaybackService;
        try {
            if (sPlayer.isPlayModeStreaming() && (iPlaybackService = gPlaybackService) != null) {
                return iPlaybackService.isPreparing();
            }
            if (sPlayer.isPlayModeRenderer() || !sPlayer.isPlayModeChromeCast() || (iChromeCastService = gChromeCastService) == null) {
                return false;
            }
            return iChromeCastService.isPreparing();
        } catch (RemoteException unused) {
            return false;
        }
    }

    public static void clearRemoteSongItem() {
        try {
            IRemoteControllerService iRemoteControllerService = gRemoteService;
            if (iRemoteControllerService != null) {
                iRemoteControllerService.clearSongItem();
            }
        } catch (RemoteException unused) {
        }
    }

    public static boolean isDownloading(SongItem song) {
        IPlaybackService iPlaybackService;
        if (song == null) {
            return false;
        }
        if (DataModelManager.getMInstance().getTaskManager().contains(song)) {
            return true;
        }
        try {
            if (sPlayer.isPlayModeStreaming() && (iPlaybackService = gPlaybackService) != null) {
                return iPlaybackService.isDownloading(song.getBundle());
            }
        } catch (RemoteException unused) {
        }
        return false;
    }

    public static boolean hasAudioToPlay() {
        IChromeCastService iChromeCastService;
        IRemoteControllerService iRemoteControllerService;
        IPlaybackService iPlaybackService;
        try {
            if (sPlayer.isPlayModeStreaming() && (iPlaybackService = gPlaybackService) != null) {
                return iPlaybackService.hasAudioToPlay();
            }
            if (sPlayer.isPlayModeRenderer() && (iRemoteControllerService = gRemoteService) != null) {
                return iRemoteControllerService.hasAudioToPlay();
            }
            if (!sPlayer.isPlayModeChromeCast() || (iChromeCastService = gChromeCastService) == null) {
                return false;
            }
            return iChromeCastService.hasAudioToPlay();
        } catch (RemoteException unused) {
            return false;
        }
    }
}