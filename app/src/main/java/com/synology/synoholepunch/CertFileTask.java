package com.synology.synoholepunch;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;


public class CertFileTask {
    private static String mCertFilePath;
    private static Context mContext;

    public static String getCertFilePath() {
        String str = mCertFilePath;
        if (str != null) {
            return str;
        }
        synchronized (CertFileTask.class) {
            String str2 = mCertFilePath;
            if (str2 != null) {
                return str2;
            }
            Context context = mContext;
            if (context == null) {
                return "";
            }
            String str3 = context.getFilesDir().getPath() + "/cacert";
            File file = new File(str3);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                KeyStore keyStore = KeyStore.getInstance("AndroidCAStore");
                if (keyStore != null) {
                    keyStore.load(null, null);
                    Enumeration<String> enumerationAliases = keyStore.aliases();
                    while (enumerationAliases.hasMoreElements()) {
                        fileOutputStream.write("-----BEGIN CERTIFICATE-----\n".getBytes());
                        fileOutputStream.write(Base64.encode(((X509Certificate) keyStore.getCertificate(enumerationAliases.nextElement())).getEncoded(), 0));
                        fileOutputStream.write("-----END CERTIFICATE-----\n".getBytes());
                    }
                }
                fileOutputStream.flush();
                fileOutputStream.close();
                Log.d("SynoPunch", "Dump cert file success, path:" + str3);
                mCertFilePath = str3;
                return str3;
            } catch (Exception unused) {
                return "";
            }
        }
    }

    public static void setContext(Context context) {
        if (context == null || context.getApplicationContext() == null) {
            throw new RuntimeException("No context for CertFileTask");
        }
        mContext = context.getApplicationContext();
    }
}