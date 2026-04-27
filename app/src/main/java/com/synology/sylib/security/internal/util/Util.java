package com.synology.sylib.security.internal.util;

import com.synology.sylib.security.util.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/* loaded from: classes.dex */
public class Util {
    public static final String TAG = "KsHelper";

    public static byte[] getSecureRandomBytes(int i) {
        return new SecureRandom().generateSeed(i);
    }

    public static byte[] getHash(byte[]... bArr) throws NoSuchAlgorithmException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            for (byte[] bArr2 : bArr) {
                messageDigest.update(bArr2);
            }
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            Logger.e("Hash", "Get Hash : " + e.getMessage(), e);
            byte[] bArr3 = new byte[32];
            int i = 0;
            int length = 0;
            while (i < 32) {
                for (byte b : bArr[length]) {
                    bArr3[i] = b;
                    if (i == 31) {
                        break;
                    }
                    i++;
                }
                length = (length + 1) % bArr.length;
            }
            return bArr3;
        }
    }
}