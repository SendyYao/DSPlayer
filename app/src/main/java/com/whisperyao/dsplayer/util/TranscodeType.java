package com.whisperyao.dsplayer.util;

import android.text.TextUtils;

import java.util.List;

public class TranscodeType {
    private static final String MP3 = "mp3";
    private static final String NONE = "none";
    private static final int RAW_VALUE_MP3 = 1;
    private static final int RAW_VALUE_WAV = 2;
    private static final String WAV = "wav";
    private int mRawValue = 0;

    public void parse(final List<String> type) {
        if (type == null || type.isEmpty()) {
            return;
        }
        for (String str : type) {
            if (!TextUtils.isEmpty(str)) {
                if ("mp3".compareToIgnoreCase(str) == 0) {
                    this.mRawValue |= 1;
                } else if (WAV.compareToIgnoreCase(str) == 0) {
                    this.mRawValue |= 2;
                }
            }
        }
    }

    public boolean supportTranscoding() {
        return this.mRawValue > 0;
    }

    public boolean supportMP3() {
        return (this.mRawValue & 1) > 0;
    }

    public boolean supportWAV() {
        return (this.mRawValue & 2) > 0;
    }

    public String getString() {
        if (supportMP3()) {
            return "mp3";
        }
        if (supportWAV()) {
            return WAV;
        }
        return "none";
    }
}