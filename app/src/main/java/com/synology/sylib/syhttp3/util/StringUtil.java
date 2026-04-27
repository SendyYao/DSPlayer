package com.synology.sylib.syhttp3.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public class StringUtil {
    public static String getCertificateSHA1String(X509Certificate x509Certificate) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(x509Certificate.getEncoded());
            return byteArrayToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new IOException(e);
        }
    }

    public static String byteArrayToHexString(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            int i = b & 255;
            if (sb.length() > 0) {
                sb.append(StringUtils.SPACE);
            }
            if (i < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(i).toUpperCase(Locale.ENGLISH));
        }
        return sb.toString();
    }
}