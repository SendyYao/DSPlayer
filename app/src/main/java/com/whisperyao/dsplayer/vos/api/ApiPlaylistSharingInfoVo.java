package com.whisperyao.dsplayer.vos.api;

import android.text.TextUtils;
import com.whisperyao.dsplayer.vos.base.BaseSharingInfoVo;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ApiPlaylistSharingInfoVo extends BaseSharingInfoVo {
    private static final String DATE_INVALID = "0";
    private static final DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String date_available;
    String date_expired;
    String id;
    ApiSharingStatus status;
    String url;

    public enum ApiSharingStatus {
        none,
        invalid,
        valid,
        expired
    }

    public static class ApiSharingStatusVo extends BaseSharingInfoVo.SharingStatusVo {
        ApiSharingStatus mStatus;

        ApiSharingStatusVo(ApiSharingStatus status) {
            this.mStatus = status;
        }

        @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo.SharingStatusVo
        public boolean isNone() {
            return this.mStatus == null || ApiSharingStatus.none.equals(this.mStatus);
        }

        @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo.SharingStatusVo
        public boolean isValid() {
            return ApiSharingStatus.valid.equals(this.mStatus);
        }

        @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo.SharingStatusVo
        public boolean isInvalid() {
            return ApiSharingStatus.invalid.equals(this.mStatus);
        }

        @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo.SharingStatusVo
        public boolean isExpired() {
            return ApiSharingStatus.expired.equals(this.mStatus);
        }
    }

    @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo
    public String getId() {
        return this.id;
    }

    @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo
    public String getUrl() {
        return this.url;
    }

    @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo
    public Date getAvailableDate() {
        if (!TextUtils.isEmpty(this.date_available) && !DATE_INVALID.equals(this.date_available)) {
            try {
                return sDateFormat.parse(this.date_available);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo
    public Date getExpiredDate() {
        if (!TextUtils.isEmpty(this.date_expired) && !DATE_INVALID.equals(this.date_expired)) {
            try {
                return sDateFormat.parse(this.date_expired);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override // com.synology.dsaudio.vos.base.BaseSharingInfoVo
    public BaseSharingInfoVo.SharingStatusVo getSharingStatus() {
        return new ApiSharingStatusVo(this.status);
    }
}