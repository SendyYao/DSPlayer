package com.whisperyao.dsplayer.vos;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class EqualizerTypeDataVo {
    private List<EqualizerType> types;

    public enum EqualizerSourceType {
        Default,
        Custom,
        Tmp
    }

    public static class EqualizerType {
        private short[] bandLevels;
        private String name;
        private final EqualizerSourceType type;

        public EqualizerType(String na, EqualizerSourceType tp, short[] levels) {
            this.name = na;
            this.type = tp;
            this.bandLevels = levels;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String na) {
            this.name = na;
        }

        public short[] getBandLevels() {
            return this.bandLevels;
        }

        public void setBandLevels(short[] levels) {
            this.bandLevels = levels;
        }

        public boolean isCustom() {
            return this.type == EqualizerSourceType.Custom;
        }

        public boolean isTmp() {
            return this.type == EqualizerSourceType.Tmp;
        }

        public String toJsonString() {
            return new Gson().toJson(this);
        }

        public static EqualizerType fromJsonString(String jsonString) {
            return new Gson().fromJson(jsonString, EqualizerType.class);
        }
    }

    public List<EqualizerType> getTypes() {
        List<EqualizerType> list = this.types;
        return list != null ? list : new ArrayList<>();
    }

    public void setTypes(List<EqualizerType> list) {
        this.types = list;
    }
}
