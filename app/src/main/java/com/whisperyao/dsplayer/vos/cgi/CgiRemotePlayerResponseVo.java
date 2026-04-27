package com.whisperyao.dsplayer.vos.cgi;

import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.vos.base.BaseRemotePlayerResponseVo;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes2.dex */
public class CgiRemotePlayerResponseVo extends BaseRemotePlayerResponseVo {
    private List<CgiRemotePlayerVo> list;

    @Override
    public List<CgiRemotePlayerVo> getRemotePlayerList() {
        return this.list;
    }

    public static class CgiRemotePlayerVo extends BaseRemotePlayerResponseVo.BaseRemotePlayerVo {
        private static final String CGI_USB_UDN = "udn_usb_speaker";
        private String friendly_name;
        private int index;
        private String ip;
        private boolean need_password;
        private String port;
        private boolean seek;
        private boolean set_volume;
        private int total = 1;
        private String type;
        private String udn;

        @Override
        public boolean isGroupPlayer() {
            return false;
        }

        @Override
        public String getId() {
            return this.udn;
        }

        @Override
        public String getName() {
            return this.friendly_name;
        }

        @Override
        public int getPlayerIndex() {
            return this.index;
        }

        @Override
        public BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType getPlayerType() {
            if (isUsbSpeaker()) {
                return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.usb;
            }
            return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.airplay;
        }

        @Override
        public boolean isUsbSpeaker() {
            return CGI_USB_UDN.equals(getId());
        }

        @Override
        public boolean isPasswordProtected() {
            return this.need_password;
        }

        @Override
        public boolean supportSeek() {
            return this.seek;
        }

        @Override
        public boolean supportSetVolume() {
            return this.set_volume;
        }

        @Override
        public List<? extends BaseRemotePlayerResponseVo.BaseRemotePlayerVo> getSubPlayerList() {
            return new ArrayList<>();
        }

        public static CgiRemotePlayerVo getUSBItem() {
            CgiRemotePlayerVo cgiRemotePlayerVo = new CgiRemotePlayerVo();
            cgiRemotePlayerVo.udn = CGI_USB_UDN;
            cgiRemotePlayerVo.friendly_name = App.getContext().getString(R.string.usb_speaker);
            cgiRemotePlayerVo.index = -1;
            return cgiRemotePlayerVo;
        }
    }
}
