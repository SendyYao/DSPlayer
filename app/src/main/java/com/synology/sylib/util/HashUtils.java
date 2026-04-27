package com.synology.sylib.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.apache.commons.lang3.CharEncoding;

/* loaded from: classes2.dex */
public class HashUtils {
    public static String getMD5Hash(String str) throws NoSuchAlgorithmException {
        if (str == null) {
            throw new IllegalArgumentException("input == null");
        }
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(str.getBytes(CharEncoding.UTF_8));
            for (byte b : messageDigest.digest()) {
                sb.append(String.format(Locale.ENGLISH, "%02x", Integer.valueOf(b & 255)));
            }
        } catch (Exception unused) {
        }
        return sb.toString();
    }

    public static String byteArrayToHexString(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            int i = b & 255;
            if (i < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(i));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String str) {
        int length = str.length();
        if (length % 2 != 0) {
            length++;
            str = "0" + str;
        }
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }
}