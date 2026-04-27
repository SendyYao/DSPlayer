package com.whisperyao.dsplayer.vos.base;

import java.util.Date;

public abstract class BaseSharingInfoVo {

    public static abstract class SharingStatusVo {
        public abstract boolean isExpired();

        public abstract boolean isInvalid();

        public abstract boolean isNone();

        public abstract boolean isValid();
    }

    public abstract Date getAvailableDate();

    public abstract Date getExpiredDate();

    public abstract String getId();

    public abstract SharingStatusVo getSharingStatus();

    public abstract String getUrl();

    public boolean isNone() {
        SharingStatusVo sharingStatus = getSharingStatus();
        return sharingStatus == null || sharingStatus.isNone();
    }

    public boolean isValid() {
        SharingStatusVo sharingStatus = getSharingStatus();
        return sharingStatus != null && sharingStatus.isValid();
    }

    public boolean isInvalid() {
        SharingStatusVo sharingStatus = getSharingStatus();
        return sharingStatus != null && sharingStatus.isInvalid();
    }

    public boolean isExpired() {
        SharingStatusVo sharingStatus = getSharingStatus();
        return sharingStatus != null && sharingStatus.isExpired();
    }
}
