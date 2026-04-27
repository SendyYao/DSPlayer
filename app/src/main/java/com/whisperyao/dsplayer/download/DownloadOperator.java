package com.whisperyao.dsplayer.download;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import java.util.HashMap;


public class DownloadOperator {
    private static HashMap<Context, ServiceBinder> gConnectionMap = new HashMap<>();
    private static IDownloadService mService = null;

    public static void startService(final Context context) {
        context.startService(new Intent(context, DownloadService.class));
    }

    public static void stopService(final Context context) {
        context.stopService(new Intent(context, DownloadService.class));
    }

    public static boolean bindService(final Context context, final ServiceConnection callback) {
        startService(context);
        ServiceBinder serviceBinder = new ServiceBinder(callback);
        gConnectionMap.put(context, serviceBinder);
        return context.bindService(new Intent().setClass(context, DownloadService.class), serviceBinder, 1);
    }

    public static void unbindFromService(final Context context) {
        ServiceBinder serviceBinderRemove = gConnectionMap.remove(context);
        if (serviceBinderRemove == null) {
            return;
        }
        context.unbindService(serviceBinderRemove);
        if (gConnectionMap.isEmpty()) {
            mService = null;
        }
    }

    public static void notifyDeleteTask() {
        IDownloadService iDownloadService = mService;
        if (iDownloadService != null) {
            try {
                iDownloadService.notifyDeleteTask();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;

        ServiceBinder(final ServiceConnection callback) {
            this.mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            if (DownloadOperator.mService == null) {
                DownloadOperator.mService = IDownloadService.Stub.asInterface(service);
            }
            ServiceConnection serviceConnection = this.mCallback;
            if (serviceConnection != null) {
                serviceConnection.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            ServiceConnection serviceConnection = this.mCallback;
            if (serviceConnection != null) {
                serviceConnection.onServiceDisconnected(className);
            }
            this.mCallback = null;
        }
    }
}
