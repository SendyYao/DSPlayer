package com.whisperyao.dsplayer.ui.login;

import com.synology.sylib.data.SynoURL;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ConnectData {
    public static final int DSM_HTTPS_PORT = 5001;
    public static final int DSM_HTTP_PORT = 5000;
    public static final int NORMAL_HTTPS_PORT = 443;
    public static final int NORMAL_HTTP_PORT = 80;
    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";
    public static final int SRM_HTTPS_PORT = 8001;
    public static final int SRM_HTTP_PORT = 8000;
    private int mExplicitPort;
    private String mInputAddress;
    private boolean mIsMalFormat;
    private String mOtpCode;
    private List<URL> mPossibleUrlList;
    private boolean mTrustDevice;
    private URL mUrl;
    private boolean mUseHTTPS;

    public ConnectData(final String inputAddress, final boolean useHTTPS, final boolean isSupportSRM, final boolean isSupportDSM) throws MalformedURLException {
        this.mExplicitPort = 0;
        this.mInputAddress = inputAddress;
        this.mUseHTTPS = useHTTPS;
        this.mPossibleUrlList = new ArrayList();
        try {
            this.mExplicitPort = new SynoURL(inputAddress).getPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            this.mIsMalFormat = true;
        }
        setupUrl(isSupportSRM, isSupportDSM);
    }

    public ConnectData(final String inputAddress, final boolean useHTTPS) throws MalformedURLException {
        this(inputAddress, useHTTPS, false, true);
    }

    public ConnectData(final String inputAddress, final boolean useHTTPS, final URL url) throws MalformedURLException {
        this(inputAddress, useHTTPS);
        this.mUrl = url;
    }

    private void setupUrl(final boolean isSupportSRM, final boolean isSupportDSM) throws MalformedURLException {
        try {
            SynoURL synoURLComposeValidURL = SynoURL.composeValidURL(this.mInputAddress, this.mUseHTTPS, 5000, 5001);
            if (synoURLComposeValidURL == null) {
                throw new MalformedURLException();
            }
            String str = this.mUseHTTPS ? "https" : "http";
            String host = synoURLComposeValidURL.getHost();
            String path = synoURLComposeValidURL.getPath();
            int i = this.mExplicitPort;
            if (i > 0) {
                this.mPossibleUrlList.add(new URL(str, host, i, path));
            } else {
                setImplicitPossibleUrlList(str, host, path, isSupportSRM, isSupportDSM);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            this.mIsMalFormat = true;
        }
    }

    private void setImplicitPossibleUrlList(final String protocol, final String host, final String path, final boolean isSupportSRM, final boolean isSupportDSM) throws MalformedURLException {
        if (this.mUseHTTPS) {
            this.mPossibleUrlList.add(new URL(protocol, host, 443, path));
            if (isSupportDSM) {
                this.mPossibleUrlList.add(new URL(protocol, host, 5001, path));
            }
            if (isSupportSRM) {
                this.mPossibleUrlList.add(new URL(protocol, host, SRM_HTTPS_PORT, path));
                return;
            }
            return;
        }
        this.mPossibleUrlList.add(new URL(protocol, host, 80, path));
        if (isSupportDSM) {
            this.mPossibleUrlList.add(new URL(protocol, host, 5000, path));
        }
        if (isSupportSRM) {
            this.mPossibleUrlList.add(new URL(protocol, host, SRM_HTTP_PORT, path));
        }
    }

    public void setOtpCode(String otpCode) {
        this.mOtpCode = otpCode;
    }

    public void setTrustDevice(boolean trustDevice) {
        this.mTrustDevice = trustDevice;
    }

    public final String getInputAddress() {
        return this.mInputAddress;
    }

    public final boolean useHTTPS() {
        return this.mUseHTTPS;
    }

    public final boolean isWrongFormat() {
        return this.mIsMalFormat;
    }

    public final void setUrl(final URL url) {
        this.mUrl = url;
    }

    public final URL getUrl() {
        return this.mUrl;
    }

    public final List<URL> getPossibleUrlList() {
        return this.mPossibleUrlList;
    }

    public final String getOtpCode() {
        return this.mOtpCode;
    }

    public final boolean isTrustDevice() {
        return this.mTrustDevice;
    }
}
