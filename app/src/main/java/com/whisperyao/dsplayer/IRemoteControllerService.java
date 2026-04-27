package com.whisperyao.dsplayer;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import java.util.Map;

/* loaded from: classes.dex */
public interface IRemoteControllerService extends IInterface {
    public static final String DESCRIPTOR = "com.synology.dsaudio.IRemoteControllerService";

    public static class Default implements IRemoteControllerService {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void clearQueue() throws RemoteException {
        }

        @Override
        public void clearSongItem() throws RemoteException {
        }

        @Override
        public void close() throws RemoteException {
        }

        @Override
        public int duration() throws RemoteException {
            return 0;
        }

        @Override
        public void enqueue(Bundle[] bundlelist, int action, int position) throws RemoteException {
        }

        @Override
        public Bundle getPlayingSongItem() throws RemoteException {
            return null;
        }

        @Override
        public Bundle getPlayingStatus() throws RemoteException {
            return null;
        }

        @Override
        public Bundle[] getQueue() throws RemoteException {
            return null;
        }

        @Override
        public int getQueuePosition() throws RemoteException {
            return 0;
        }

        @Override
        public int getQueueSize() throws RemoteException {
            return 0;
        }

        @Override
        public String getRepeatMode() throws RemoteException {
            return null;
        }

        @Override
        public String getShuffleMode() throws RemoteException {
            return null;
        }

        @Override
        public int getVolume() throws RemoteException {
            return 0;
        }

        @Override
        public boolean hasAudioToPlay() throws RemoteException {
            return false;
        }

        @Override
        public boolean isPause() throws RemoteException {
            return false;
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return false;
        }

        @Override
        public boolean isPlayingRadio() throws RemoteException {
            return false;
        }

        @Override
        public void next() throws RemoteException {
        }

        @Override
        public void pause() throws RemoteException {
        }

        @Override
        public void play() throws RemoteException {
        }

        @Override
        public int position() throws RemoteException {
            return 0;
        }

        @Override
        public void prev() throws RemoteException {
        }

        @Override
        public boolean queueInitiated() throws RemoteException {
            return false;
        }

        @Override
        public void removeTracks(Bundle[] bundlelist, int[] ids) throws RemoteException {
        }

        @Override
        public void replayCurrent() throws RemoteException {
        }

        @Override
        public void seek(int pos) throws RemoteException {
        }

        @Override
        public void setQueuePosition(int index) throws RemoteException {
        }

        @Override
        public void setRepeatMode(String repeatmode) throws RemoteException {
        }

        @Override
        public void setShuffleMode(String shufflemode) throws RemoteException {
        }

        @Override
        public void setSubPlayersVolume(Map subplayersVolume) throws RemoteException {
        }

        @Override
        public void setVolume(int volume) throws RemoteException {
        }

        @Override
        public void stop() throws RemoteException {
        }

        @Override
        public void updateTracks(Bundle[] bundlelist, int start, int limit, int[] ids) throws RemoteException {
        }
    }

    void clearQueue() throws RemoteException;

    void clearSongItem() throws RemoteException;

    void close() throws RemoteException;

    int duration() throws RemoteException;

    void enqueue(Bundle[] bundlelist, int action, int position) throws RemoteException;

    Bundle getPlayingSongItem() throws RemoteException;

    Bundle getPlayingStatus() throws RemoteException;

    Bundle[] getQueue() throws RemoteException;

    int getQueuePosition() throws RemoteException;

    int getQueueSize() throws RemoteException;

    String getRepeatMode() throws RemoteException;

    String getShuffleMode() throws RemoteException;

    int getVolume() throws RemoteException;

    boolean hasAudioToPlay() throws RemoteException;

    boolean isPause() throws RemoteException;

    boolean isPlaying() throws RemoteException;

    boolean isPlayingRadio() throws RemoteException;

    void next() throws RemoteException;

    void pause() throws RemoteException;

    void play() throws RemoteException;

    int position() throws RemoteException;

    void prev() throws RemoteException;

    boolean queueInitiated() throws RemoteException;

    void removeTracks(Bundle[] bundlelist, int[] ids) throws RemoteException;

    void replayCurrent() throws RemoteException;

    void seek(int pos) throws RemoteException;

    void setQueuePosition(int index) throws RemoteException;

    void setRepeatMode(String repeatmode) throws RemoteException;

    void setShuffleMode(String shufflemode) throws RemoteException;

    void setSubPlayersVolume(Map subplayersVolume) throws RemoteException;

    void setVolume(int volume) throws RemoteException;

    void stop() throws RemoteException;

    void updateTracks(Bundle[] bundlelist, int start, int limit, int[] ids) throws RemoteException;

    public static abstract class Stub extends Binder implements IRemoteControllerService {
        static final int TRANSACTION_clearQueue = 26;
        static final int TRANSACTION_clearSongItem = 30;
        static final int TRANSACTION_close = 32;
        static final int TRANSACTION_duration = 10;
        static final int TRANSACTION_enqueue = 23;
        static final int TRANSACTION_getPlayingSongItem = 19;
        static final int TRANSACTION_getPlayingStatus = 20;
        static final int TRANSACTION_getQueue = 24;
        static final int TRANSACTION_getQueuePosition = 1;
        static final int TRANSACTION_getQueueSize = 25;
        static final int TRANSACTION_getRepeatMode = 18;
        static final int TRANSACTION_getShuffleMode = 16;
        static final int TRANSACTION_getVolume = 29;
        static final int TRANSACTION_hasAudioToPlay = 31;
        static final int TRANSACTION_isPause = 3;
        static final int TRANSACTION_isPlaying = 2;
        static final int TRANSACTION_isPlayingRadio = 14;
        static final int TRANSACTION_next = 9;
        static final int TRANSACTION_pause = 5;
        static final int TRANSACTION_play = 6;
        static final int TRANSACTION_position = 11;
        static final int TRANSACTION_prev = 7;
        static final int TRANSACTION_queueInitiated = 33;
        static final int TRANSACTION_removeTracks = 21;
        static final int TRANSACTION_replayCurrent = 8;
        static final int TRANSACTION_seek = 12;
        static final int TRANSACTION_setQueuePosition = 13;
        static final int TRANSACTION_setRepeatMode = 17;
        static final int TRANSACTION_setShuffleMode = 15;
        static final int TRANSACTION_setSubPlayersVolume = 27;
        static final int TRANSACTION_setVolume = 28;
        static final int TRANSACTION_stop = 4;
        static final int TRANSACTION_updateTracks = 22;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IRemoteControllerService.DESCRIPTOR);
        }

        public static IRemoteControllerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = obj.queryLocalInterface(IRemoteControllerService.DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IRemoteControllerService)) {
                return (IRemoteControllerService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(obj);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IRemoteControllerService.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IRemoteControllerService.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    int queuePosition = getQueuePosition();
                    parcel2.writeNoException();
                    parcel2.writeInt(queuePosition);
                    return true;
                case 2:
                    boolean zIsPlaying = isPlaying();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPlaying ? 1 : 0);
                    return true;
                case 3:
                    boolean zIsPause = isPause();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPause ? 1 : 0);
                    return true;
                case 4:
                    stop();
                    parcel2.writeNoException();
                    return true;
                case 5:
                    pause();
                    parcel2.writeNoException();
                    return true;
                case 6:
                    play();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    prev();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    replayCurrent();
                    parcel2.writeNoException();
                    return true;
                case 9:
                    next();
                    parcel2.writeNoException();
                    return true;
                case 10:
                    int iDuration = duration();
                    parcel2.writeNoException();
                    parcel2.writeInt(iDuration);
                    return true;
                case 11:
                    int iPosition = position();
                    parcel2.writeNoException();
                    parcel2.writeInt(iPosition);
                    return true;
                case 12:
                    seek(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    setQueuePosition(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    boolean zIsPlayingRadio = isPlayingRadio();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPlayingRadio ? 1 : 0);
                    return true;
                case 15:
                    setShuffleMode(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 16:
                    String shuffleMode = getShuffleMode();
                    parcel2.writeNoException();
                    parcel2.writeString(shuffleMode);
                    return true;
                case 17:
                    setRepeatMode(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 18:
                    String repeatMode = getRepeatMode();
                    parcel2.writeNoException();
                    parcel2.writeString(repeatMode);
                    return true;
                case 19:
                    Bundle playingSongItem = getPlayingSongItem();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, playingSongItem, 1);
                    return true;
                case 20:
                    Bundle playingStatus = getPlayingStatus();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, playingStatus, 1);
                    return true;
                case 21:
                    removeTracks((Bundle[]) parcel.createTypedArray(Bundle.CREATOR), parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case 22:
                    updateTracks((Bundle[]) parcel.createTypedArray(Bundle.CREATOR), parcel.readInt(), parcel.readInt(), parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case 23:
                    enqueue((Bundle[]) parcel.createTypedArray(Bundle.CREATOR), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 24:
                    Bundle[] queue = getQueue();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(queue, 1);
                    return true;
                case 25:
                    int queueSize = getQueueSize();
                    parcel2.writeNoException();
                    parcel2.writeInt(queueSize);
                    return true;
                case 26:
                    clearQueue();
                    parcel2.writeNoException();
                    return true;
                case 27:
                    setSubPlayersVolume(parcel.readHashMap(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case 28:
                    setVolume(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 29:
                    int volume = getVolume();
                    parcel2.writeNoException();
                    parcel2.writeInt(volume);
                    return true;
                case 30:
                    clearSongItem();
                    parcel2.writeNoException();
                    return true;
                case 31:
                    boolean zHasAudioToPlay = hasAudioToPlay();
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasAudioToPlay ? 1 : 0);
                    return true;
                case 32:
                    close();
                    parcel2.writeNoException();
                    return true;
                case 33:
                    boolean zQueueInitiated = queueInitiated();
                    parcel2.writeNoException();
                    parcel2.writeInt(zQueueInitiated ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IRemoteControllerService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IRemoteControllerService.DESCRIPTOR;
            }

            @Override
            public int getQueuePosition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPlaying() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPause() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stop() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void pause() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void play() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void prev() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void replayCurrent() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void next() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int duration() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int position() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void seek(int pos) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeInt(pos);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setQueuePosition(int index) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeInt(index);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPlayingRadio() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setShuffleMode(String shufflemode) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeString(shufflemode);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getShuffleMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRepeatMode(String repeatmode) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeString(repeatmode);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getRepeatMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getPlayingSongItem() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Bundle) _Parcel.readTypedObject(parcelObtain2, Bundle.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getPlayingStatus() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Bundle) _Parcel.readTypedObject(parcelObtain2, Bundle.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeTracks(Bundle[] bundlelist, int[] ids) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeTypedArray(bundlelist, 0);
                    parcelObtain.writeIntArray(ids);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateTracks(Bundle[] bundlelist, int start, int limit, int[] ids) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeTypedArray(bundlelist, 0);
                    parcelObtain.writeInt(start);
                    parcelObtain.writeInt(limit);
                    parcelObtain.writeIntArray(ids);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enqueue(Bundle[] bundlelist, int action, int position) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeTypedArray(bundlelist, 0);
                    parcelObtain.writeInt(action);
                    parcelObtain.writeInt(position);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle[] getQueue() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Bundle[]) parcelObtain2.createTypedArray(Bundle.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getQueueSize() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearQueue() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSubPlayersVolume(Map subplayersVolume) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeMap(subplayersVolume);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setVolume(int volume) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    parcelObtain.writeInt(volume);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getVolume() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearSongItem() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasAudioToPlay() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void close() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean queueInitiated() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IRemoteControllerService.DESCRIPTOR);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        private static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> c) {
            if (parcel.readInt() != 0) {
                return c.createFromParcel(parcel);
            }
            return null;
        }
        
        private static <T extends Parcelable> void writeTypedObject(Parcel parcel, T value, int parcelableFlags) {
            if (value != null) {
                parcel.writeInt(1);
                value.writeToParcel(parcel, parcelableFlags);
            } else {
                parcel.writeInt(0);
            }
        }
    }
}
