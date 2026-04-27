package com.whisperyao.dsplayer.datasource.network.api;

import android.util.SparseIntArray;
import com.whisperyao.dsplayer.R;

/* loaded from: classes.dex */
public abstract class BaseWebApi implements WebApi {
    public static final int VO_EMPTY_VO = 1000;
    public static final int WEBAPI_AUTH_ERR_ACC_LOCKED = 411;
    public static final int WEBAPI_AUTH_ERR_ACC_PASS_ERR = 400;
    public static final int WEBAPI_AUTH_ERR_AUTO_BLOCK_MAX_TRIES = 407;
    public static final int WEBAPI_AUTH_ERR_NO_PRIVILEGE = 402;
    public static final int WEBAPI_AUTH_ERR_OTP_ENFORCED = 406;
    public static final int WEBAPI_AUTH_ERR_OTP_INVALID = 404;
    public static final int WEBAPI_AUTH_ERR_OTP_REQUIRE = 403;
    public static final int WEBAPI_AUTH_ERR_PORTAL_PORT_INVALID = 405;
    public static final int WEBAPI_AUTH_ERR_PWD_EXPIRED = 409;
    public static final int WEBAPI_AUTH_ERR_PWD_EXPIRED_CANT_CHANGE = 408;
    public static final int WEBAPI_AUTH_ERR_PWD_MUST_CHANGE = 410;
    public static final int WEBAPI_ERR_BAD_REQUEST = 101;
    public static final int WEBAPI_ERR_COMPOUND_REJECT = 113;
    public static final int WEBAPI_ERR_COMPOUND_STOP = 112;
    public static final int WEBAPI_ERR_HANDLE_UPLOAD = 108;
    public static final int WEBAPI_ERR_INTERNAL_ERROR = 117;
    public static final int WEBAPI_ERR_NOT_ALLOW_DEMO = 116;
    public static final int WEBAPI_ERR_NOT_ALLOW_UPLOAD = 115;
    public static final int WEBAPI_ERR_NOT_SUPPORTED_VERSION = 104;
    public static final int WEBAPI_ERR_NO_APP_PRIVILEGE = 160;
    public static final int WEBAPI_ERR_NO_MATCH_LIB_ENTRY = 121;
    public static final int WEBAPI_ERR_NO_PERMISSION = 105;
    public static final int WEBAPI_ERR_NO_REQUIRED_PARAM = 114;
    public static final int WEBAPI_ERR_NO_SUCH_API = 102;
    public static final int WEBAPI_ERR_NO_SUCH_METHOD = 103;
    public static final int WEBAPI_ERR_PROCESS_ENTRY = 110;
    public static final int WEBAPI_ERR_PROCESS_LIB = 111;
    public static final int WEBAPI_ERR_PROCESS_NAME_ERROR = 118;
    public static final int WEBAPI_ERR_PROCESS_RELAY = 109;
    public static final int WEBAPI_ERR_REQUEST_PARAMETER_INVALID = 120;
    public static final int WEBAPI_ERR_SESSION_INTERRUPT = 107;
    public static final int WEBAPI_ERR_SESSION_TIMEOUT = 106;
    public static final int WEBAPI_ERR_SID_NOT_FOUND = 119;
    public static final int WEBAPI_ERR_UNKNOWN = 100;
    public static SparseIntArray sApiMap;

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        sApiMap = sparseIntArray;
        sparseIntArray.put(100, R.string.error_system);
        sApiMap.put(101, R.string.error_connect_fail);
        sApiMap.put(102, R.string.error_system);
        sApiMap.put(103, R.string.error_system);
        sApiMap.put(104, R.string.error_system);
        sApiMap.put(105, R.string.error_noprivilege);
        sApiMap.put(106, R.string.error_network_not_available);
        sApiMap.put(107, R.string.error_system);
        sApiMap.put(108, R.string.error_system);
        sApiMap.put(109, R.string.error_system);
        sApiMap.put(110, R.string.error_system);
        sApiMap.put(111, R.string.error_system);
        sApiMap.put(112, R.string.error_system);
        sApiMap.put(113, R.string.error_system);
        sApiMap.put(114, R.string.error_invalid);
        sApiMap.put(115, R.string.error_invalid);
        sApiMap.put(116, R.string.error_system);
        sApiMap.put(117, R.string.error_system);
        sApiMap.put(118, R.string.error_system);
        sApiMap.put(119, R.string.error_privilege_not_enough);
        sApiMap.put(120, R.string.error_system);
        sApiMap.put(121, R.string.error_system);
        sApiMap.put(WEBAPI_ERR_NO_APP_PRIVILEGE, R.string.error__login__no_privilege__action);
        sApiMap.put(1000, R.string.error_system);
        sApiMap.put(400, R.string.login_error_account);
        sApiMap.put(402, R.string.error_noprivilege);
        sApiMap.put(403, R.string.enter_otp_code);
        sApiMap.put(404, R.string.error_otp_incorrect);
        sApiMap.put(405, R.string.error_auth_port_invalid);
        sApiMap.put(406, R.string.error_otp_enforced);
        sApiMap.put(407, R.string.error_max_tries);
        sApiMap.put(408, R.string.error_pwd_expired);
        sApiMap.put(409, R.string.error_pwd_expired);
        sApiMap.put(410, R.string.error_pwd_must_change);
        sApiMap.put(411, R.string.error_account_locked);
    }

    @Override
    public int errorStringRes(int errorCode) {
        if (sApiMap.get(errorCode) > 0) {
            return sApiMap.get(errorCode);
        }
        return sApiMap.get(100);
    }

    @Override
    public int[] supportedVersion() {
        return new int[]{1};
    }
}
