package com.whisperyao.dsplayer.item;

import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.vos.base.BaseRemotePlayerResponseVo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class RendererItem {
    private String mId;
    private boolean mIsGroupPlayer;
    private boolean mIsPasswordProtected;
    private String mName;
    private boolean mSupportSeek;
    private boolean mSupportSetVolume;
    private int mIndex = -1;
    private BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType mPlayerType = BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.unknown;
    private List<RendererItem> mSubPlayers = new ArrayList();

    public static RendererItem fromRemotePlayerResponseVo(BaseRemotePlayerResponseVo.BaseRemotePlayerVo remotePlayerVo) {
        RendererItem rendererItem = new RendererItem();
        boolean zIsUsbSpeaker = remotePlayerVo.isUsbSpeaker();
        rendererItem.mId = remotePlayerVo.getId();
        rendererItem.mName = zIsUsbSpeaker ? App.getContext().getString(R.string.usb_speaker) : remotePlayerVo.getName();
        rendererItem.mIndex = remotePlayerVo.getPlayerIndex();
        rendererItem.mPlayerType = remotePlayerVo.getPlayerType();
        rendererItem.mSupportSeek = remotePlayerVo.supportSeek();
        rendererItem.mSupportSetVolume = remotePlayerVo.supportSetVolume();
        rendererItem.mIsPasswordProtected = remotePlayerVo.isPasswordProtected();
        rendererItem.mIsGroupPlayer = remotePlayerVo.isGroupPlayer();
        List<? extends BaseRemotePlayerResponseVo.BaseRemotePlayerVo> subPlayerList = remotePlayerVo.getSubPlayerList();
        ArrayList arrayList = new ArrayList();
        Iterator<? extends BaseRemotePlayerResponseVo.BaseRemotePlayerVo> it = subPlayerList.iterator();
        while (it.hasNext()) {
            arrayList.add(fromRemotePlayerResponseVo(it.next()));
        }
        rendererItem.mSubPlayers = arrayList;
        return rendererItem;
    }

    public String getUniqueId() {
        return this.mId;
    }

    public int getIndex() {
        return this.mIndex;
    }

    public BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType getPlayerType() {
        return this.mPlayerType;
    }

    public String getName() {
        return this.mName;
    }

    public boolean hasPassword() {
        return this.mIsPasswordProtected;
    }

    public boolean isGroupPlayer() {
        return this.mIsGroupPlayer;
    }

    public List<RendererItem> getSubPlayers() {
        return this.mSubPlayers;
    }
}
