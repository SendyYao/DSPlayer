package com.whisperyao.dsplayer.download;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IDownloadService extends IInterface {
    String DESCRIPTOR = "com.synology.dsaudio.download.IDownloadService";

    class Default implements IDownloadService {
        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public void notifyDeleteTask() throws RemoteException {
        }
    }

    void notifyDeleteTask() throws RemoteException;

    public static abstract class Stub extends Binder implements IDownloadService {
        static final int TRANSACTION_notifyDeleteTask = 1;

        @Override
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IDownloadService.DESCRIPTOR);
        }

        public static IDownloadService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = obj.queryLocalInterface(IDownloadService.DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IDownloadService)) {
                return (IDownloadService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(obj);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IDownloadService.DESCRIPTOR);
            }
            if (code == 1598968902) {
                reply.writeString(IDownloadService.DESCRIPTOR);
                return true;
            }
            if (code == 1) {
                notifyDeleteTask();
                reply.writeNoException();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IDownloadService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDownloadService.DESCRIPTOR;
            }

            @Override
            public void notifyDeleteTask() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IDownloadService.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
