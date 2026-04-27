package com.whisperyao.dsplayer;

import android.content.Context;
import com.synology.sylib.util.DeviceUtil;


public class StateManager {
    static final boolean assertionsDisabled = false;
    private static StateManager sStateManager;
    private boolean mIsMobile;

    public boolean isMobileLayout() {
        return true;
    }

    private StateManager(Context context) {
        this.mIsMobile = DeviceUtil.isMobile(context);
    }

    public static void initInstance(Context context) {
        sStateManager = new StateManager(context);
    }

    public static StateManager getInstance() {
        if (sStateManager == null) {
            initInstance(App.getContext());
        }
        return sStateManager;
    }

    public boolean isMobile() {
        return this.mIsMobile;
    }
}