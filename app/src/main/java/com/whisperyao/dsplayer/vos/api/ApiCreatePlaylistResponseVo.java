package com.whisperyao.dsplayer.vos.api;

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;
import com.whisperyao.dsplayer.vos.base.BaseCreatePlaylistResponseVo;

public class ApiCreatePlaylistResponseVo extends BaseCreatePlaylistResponseVo {
    private static final int ERROR_CODE_FILE_EXISTS = 406;
    private ApiCreatePlaylistResponseDataVo data;

    @Override
    public String getId() {
        ApiCreatePlaylistResponseDataVo apiCreatePlaylistResponseDataVo = this.data;
        if (apiCreatePlaylistResponseDataVo == null) {
            return null;
        }
        return apiCreatePlaylistResponseDataVo.id;
    }

    @Override
    public boolean isErrorPlaylistExist() {
        BaseVo.ErrorCodeVo error;
        return !getSuccess() && (error = getError()) != null && error.getCode() == 406;
    }

    private static class ApiCreatePlaylistResponseDataVo {
        private String id;

        private ApiCreatePlaylistResponseDataVo() {
        }
    }
}
