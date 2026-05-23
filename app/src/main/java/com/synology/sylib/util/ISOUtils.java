package com.synology.sylib.util;

import android.content.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ISOUtils {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final String DEFAULT_REMOTE_LANGUAGE_CODE = "enu";

    public static String getLanguageString(Context context) {
        return getLanguageCodeInRemoteFormat(context);
    }

    public static String getLocaleString(Context context) {
        return getLanguageCodeInLocalFormat(context);
    }

    private static String getLanguageCodeInRemoteFormat(Context context) {
        Locale locale = getLocale(context);
        Map<Locale, String> supportedLocaleMap = getSupportedLocaleMap();
        if (supportedLocaleMap.containsKey(locale)) {
            return supportedLocaleMap.get(locale);
        }
        return DEFAULT_REMOTE_LANGUAGE_CODE;
    }

    private static String getLanguageCodeInLocalFormat(Context context) {
        return getLocale(context).toString();
    }

    private static Locale getLocale(Context context) {
        if (context == null) {
            return DEFAULT_LOCALE;
        }
        Locale configurationLocale = getConfigurationLocale(context);
        Locale locale = new Locale(configurationLocale.getLanguage());
        Locale locale2 = new Locale(configurationLocale.getLanguage(), configurationLocale.getCountry());
        Map<Locale, String> supportedLocaleMap = getSupportedLocaleMap();
        return supportedLocaleMap.containsKey(locale2) ? locale2 : supportedLocaleMap.containsKey(locale) ? locale : DEFAULT_LOCALE;
    }

    private static Locale getConfigurationLocale(Context context) {
        if (context == null) {
            return DEFAULT_LOCALE;
        }
        return context.getResources().getConfiguration().locale;
    }

    private static Map<Locale, String> getSupportedLocaleMap() {
        HashMap<Locale, String> map = new HashMap<>();
        map.put(new Locale("cs"), "csy");
        map.put(new Locale("da"), "dan");
        map.put(new Locale("nl"), "nld");
        map.put(new Locale("en"), DEFAULT_REMOTE_LANGUAGE_CODE);
        map.put(new Locale("fr"), "fre");
        map.put(new Locale("de"), "ger");
        map.put(new Locale("hu"), "hun");
        map.put(new Locale("it"), "ita");
        map.put(new Locale("ja"), "jpn");
        map.put(new Locale("ko"), "krn");
        map.put(new Locale("no"), "nor");
        map.put(new Locale("nb"), "nor");
        map.put(new Locale("nn"), "nor");
        map.put(new Locale("pl"), "plk");
        map.put(new Locale("pt", "BR"), "ptb");
        map.put(new Locale("pt"), "ptg");
        map.put(new Locale("ru"), "rus");
        map.put(new Locale("zh", "CN"), "chs");
        map.put(new Locale("zh", "TW"), "cht");
        map.put(new Locale("zh", "HK"), "cht");
        map.put(new Locale("es"), "spn");
        map.put(new Locale("sv"), "sve");
        map.put(new Locale("th"), "tha");
        map.put(new Locale("tr"), "trk");
        return map;
    }
}
