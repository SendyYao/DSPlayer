package com.whisperyao.dsplayer.vos.api;

import com.whisperyao.dsplayer.vos.base.BaseRemotePlayerResponseVo;
import java.util.ArrayList;
import java.util.List;

public class ApiRemotePlayerResponseVo extends BaseRemotePlayerResponseVo {
    private ApiRemotePlayerResponseDataVo data;

    public static class ApiRemotePlayerAdditionalVo {
        List<ApiRemotePlayerVo> subplayer_list;
    }

    public static class ApiRemotePlayerResponseDataVo {
        List<ApiRemotePlayerVo> players;
    }

    private enum ApiRemotePlayerType {
        usb,
        bluetooth,
        upnp,
        airplay
    }

    @Override // com.synology.dsaudio.vos.base.BaseRemotePlayerResponseVo
    public List<ApiRemotePlayerVo> getRemotePlayerList() {
        return this.data.players;
    }

    public static class ApiRemotePlayerVo extends BaseRemotePlayerResponseVo.BaseRemotePlayerVo {
        private static final String API_USB_ID = "__SYNO_USB_PLAYER__";
        ApiRemotePlayerAdditionalVo additional;
        String id;
        boolean is_multiple;
        String name;
        boolean password_protected;
        boolean support_seek;
        boolean support_set_volume;
        ApiRemotePlayerType type;

        @Override
        public int getPlayerIndex() {
            return 0;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType getPlayerType() {
            BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType baseRemotePlayerType = BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.unknown;
            if (this.type == ApiRemotePlayerType.usb) {
                return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.usb;
            }
            if (this.type == ApiRemotePlayerType.bluetooth) {
                return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.bluetooth;
            }
            if (this.type == ApiRemotePlayerType.upnp) {
                return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.upnp;
            }
            if (this.type == ApiRemotePlayerType.airplay) {
                return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.airplay;
            }
            return BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.unknown;
        }

        @Override
        public boolean isUsbSpeaker() {
            return API_USB_ID.equals(this.id);
        }

        @Override
        public boolean isGroupPlayer() {
            return this.is_multiple;
        }

        @Override
        public boolean isPasswordProtected() {
            return this.password_protected;
        }

        @Override
        public boolean supportSeek() {
            return this.support_seek;
        }

        @Override
        public boolean supportSetVolume() {
            return this.support_set_volume;
        }

        @Override
        public List<ApiRemotePlayerVo> getSubPlayerList() {
            ApiRemotePlayerAdditionalVo apiRemotePlayerAdditionalVo = this.additional;
            if (apiRemotePlayerAdditionalVo != null) {
                return apiRemotePlayerAdditionalVo.subplayer_list;
            }
            return new ArrayList<>();
        }
    }
}
