package com.synology.sylib.data;

import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/* loaded from: classes.dex */
public class SynoURL {
    private static final String IPV4_PATTERN_NON_CAPTURE = "(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9]))";
    private static final String IPV4_TRAILING_DOT_PATTERN = "((?:.+://)?)((?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))\\.*((?::.*|/.*)?)";
    public static final String PROTOCOL_HTTP = "http://";
    public static final String PROTOCOL_HTTPS = "https://";
    public static final String SCHEME_HTTP = "http";
    public static final String SCHEME_HTTPS = "https";
    private final URL url;
    private boolean useDefaultPort;

    private String getIDNEncodedHostname(String str) throws MalformedURLException {
        try {
            String[] strArrSplit = str.split(":");
            if (strArrSplit.length <= 0) {
                throw new MalformedURLException();
            }
            strArrSplit[0] = IDN.toASCII(strArrSplit[0]);
            return StringUtils.join(strArrSplit, ":");
        } catch (IllegalArgumentException unused) {
            throw new MalformedURLException();
        }
    }

    private String getIDNEncodedAddress(String str) throws MalformedURLException {
//        RemoteSettings.FORWARD_SLASH_STRING
        String[] strArrSplit = str.split("/");
        if (3 > strArrSplit.length) {
            return str;
        }
        strArrSplit[2] = getIDNEncodedHostname(strArrSplit[2]);
//        RemoteSettings.FORWARD_SLASH_STRING
        return StringUtils.join(strArrSplit, "/");
    }

    private String getUnifiedAddress(String str) throws MalformedURLException {
        if (!str.toLowerCase().startsWith("http://") && !str.toLowerCase().startsWith("https://")) {
            if (!str.startsWith("[") && 2 < str.split(":").length) {
                str = "[" + str + "]";
            }
            str = "http://" + str;
        }
        return getIDNEncodedAddress(str);
    }

    private static String trimIPv4TrailingDot(String str) {
        Matcher matcher = Pattern.compile(IPV4_TRAILING_DOT_PATTERN).matcher(str);
        if (!matcher.matches()) {
            return str;
        }
        String strGroup = matcher.group(1);
        String strGroup2 = matcher.group(2);
        String strGroup3 = matcher.group(3);
        if (strGroup3 == null) {
            strGroup3 = "";
        }
        return strGroup + strGroup2 + strGroup3;
    }

    public SynoURL(String str) throws MalformedURLException {
        this(str, true);
    }

    public SynoURL(String str, boolean z) throws MalformedURLException {
        this.url = generateURL(cutQuickConnect(str), z);
    }

    public SynoURL(String str, String str2, int i) throws MalformedURLException {
        this(str, str2, i, true);
    }

    public SynoURL(String protocol, String address, int defaultPort, boolean convertIPv4)
            throws MalformedURLException {

        SynoURL synoURL = new SynoURL(address, convertIPv4);

        int port = synoURL.getPort();
        if (port <= 0) {
            port = defaultPort;
        }

        String fullUrl = String.format(
                "%s://%s:%d%s",
                protocol,
                synoURL.getHost(),
                port,
                synoURL.getPath()
        );

        this.url = generateURL(fullUrl, convertIPv4);
        this.useDefaultPort = (port == defaultPort);
    }

    private URL generateURL(String str, boolean z) throws MalformedURLException {
        String strTrimIPv4TrailingDot = trimIPv4TrailingDot(getUnifiedAddress(str));
        if (z) {
            return convertIPv4MappingUrl(new URL(strTrimIPv4TrailingDot));
        }
        return new URL(strTrimIPv4TrailingDot);
    }

    public boolean isUseDefaultPort() {
        return this.useDefaultPort;
    }

    public URL getURL() {
        return this.url;
    }

    public String getHost() {
        return this.url.getHost();
    }

    public int getPort() {
        return this.url.getPort();
    }

    public String getPath() {
        return this.url.getPath();
    }

    public boolean isHttps() {
        return this.url.getProtocol().equalsIgnoreCase("https");
    }

    public String getOriginalHost() {
        return IDN.toUnicode(getHost());
    }

    public static SynoURL composeValidURL(String str, boolean z, int i, int i2) {
        return composeValidURL(str, z, i, i2, true);
    }

    public static SynoURL composeValidURL(String str, boolean z, int i, int i2, boolean z2) {
        String str2 = z ? "https" : "http";
        if (z) {
            i = i2;
        }
        try {
            return new SynoURL(str2, str, i, z2);
        } catch (MalformedURLException unused) {
            return null;
        }
    }

    public static boolean compareUrlIgnoreHttps(SynoURL synoURL, SynoURL synoURL2) {
        if (synoURL == null && synoURL2 == null) {
            return true;
        }
        if (synoURL == null || synoURL2 == null) {
            return false;
        }
        boolean zEqualsIgnoreCase = synoURL.getOriginalHost().equalsIgnoreCase(synoURL2.getOriginalHost());
        return (synoURL.isUseDefaultPort() && synoURL2.isUseDefaultPort()) ? zEqualsIgnoreCase : zEqualsIgnoreCase && synoURL.getPort() == synoURL2.getPort();
    }

    public static URL convertIPv4MappingUrl(URL url) {
        Matcher matcher = Pattern.compile("^\\[?::[fF]{4}:((\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3}))\\]?$").matcher(url.getHost());
        if (matcher.find()) {
            for (int i = 2; i <= 5; i++) {
                if (Integer.parseInt(matcher.group(i)) > 255) {
                    return url;
                }
            }
            try {
                return new URL(url.getProtocol(), matcher.group(1), url.getPort(), url.getFile());
            } catch (MalformedURLException unused) {
            }
        }
        return url;
    }

    public static String cutQuickConnect(String str) {
        return matchQCPattern(matchQCPattern(matchQCPattern(str, Pattern.compile("(?i)(?:.+\\.)?(.+)\\..+\\.quickconnect\\.(?:to|cn)(?::\\d+)?")), Pattern.compile("(?i)([^.]+)\\.quickconnect\\.(?:to|cn)(?::\\d+)?")), Pattern.compile("(?i)quickconnect\\.(?:to|cn)(?::\\d+)?/([^/]+)"));
    }

    private static String matchQCPattern(String str, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        return matcher.find() ? str.replaceAll(pattern.pattern(), matcher.group(1)) : str;
    }

    public static boolean isUrlHostIPv4WithTrailingDot(String str) {
        return trimIPv4TrailingDot(str).length() != str.length();
    }

    private static boolean isQuickConnectId(String str) {
        return !TextUtils.isEmpty(str) && str.indexOf(46) < 0 && str.indexOf(58) < 0;
    }

    public static void handleAddressUnfocus(TextView textView, CheckBox checkBox) {
        checkBox.setChecked(internalHandleAddressUnfocus(textView, checkBox));
    }

    public static void handleAddressUnfocus(TextView textView, SwitchMaterial switchMaterial) {
        switchMaterial.setChecked(internalHandleAddressUnfocus(textView, switchMaterial));
    }

    private static boolean internalHandleAddressUnfocus(TextView textView, CompoundButton compoundButton) {
        String strTrimIPv4TrailingDot = trimIPv4TrailingDot(textView.getText().toString());
        boolean zIsChecked = compoundButton.isChecked();
        Matcher matcher = Pattern.compile("(https?)://(.*)", 2).matcher(strTrimIPv4TrailingDot);
        if (matcher.matches()) {
            zIsChecked = matcher.group(1).equalsIgnoreCase("https");
            strTrimIPv4TrailingDot = matcher.group(2);
        }
        try {
            SynoURL synoURL = new SynoURL(strTrimIPv4TrailingDot);
            if (isQuickConnectId(synoURL.getHost())) {
                strTrimIPv4TrailingDot = synoURL.getHost();
                if (!TextUtils.isEmpty(synoURL.getPath())) {
                    strTrimIPv4TrailingDot = strTrimIPv4TrailingDot + synoURL.getPath();
                }
            }
        } catch (MalformedURLException unused) {
        }
        textView.setText(strTrimIPv4TrailingDot);
        return zIsChecked;
    }
}
