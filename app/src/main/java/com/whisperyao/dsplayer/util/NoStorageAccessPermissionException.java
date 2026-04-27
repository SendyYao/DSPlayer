package com.whisperyao.dsplayer.util;


import java.io.IOException;

public class NoStorageAccessPermissionException extends IOException {
    public NoStorageAccessPermissionException(String reason) {
        super(reason);
    }

    public NoStorageAccessPermissionException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public NoStorageAccessPermissionException(Throwable cause) {
        super(cause == null ? null : cause.toString());
        initCause(cause);
    }
}
