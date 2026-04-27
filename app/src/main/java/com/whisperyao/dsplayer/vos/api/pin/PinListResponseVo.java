package com.whisperyao.dsplayer.vos.api.pin;

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;

import java.util.List;

public class PinListResponseVo extends BaseVo {
    private PinListData data;

    private class PinListData {
        private List<PinItemVo> items;
        private int offset;
        private int total = 0;

        private PinListData() {
        }
    }

    public List<PinItemVo> getItems() {
        PinListData pinListData = this.data;
        if (pinListData == null || pinListData.items == null) {
            return null;
        }
        return this.data.items;
    }

    public int getTotal() {
        PinListData pinListData = this.data;
        if (pinListData != null) {
            return pinListData.total;
        }
        return 0;
    }
}
