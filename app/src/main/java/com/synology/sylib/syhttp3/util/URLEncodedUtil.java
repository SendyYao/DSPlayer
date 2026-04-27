package com.synology.sylib.syhttp3.util;

import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.lang3.CharEncoding;


public class URLEncodedUtil {
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String NAME_VALUE_SEPARATOR = "=";
    private static final String PARAMETER_SEPARATOR = "&";

    public static List<BasicKeyValuePair> parse(URI uri, String str) {
        List<BasicKeyValuePair> listEmptyList = Collections.emptyList();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.length() <= 0) {
            return listEmptyList;
        }
        ArrayList arrayList = new ArrayList();
        parse(arrayList, new Scanner(rawQuery), str);
        return arrayList;
    }

    public static void parse(List<BasicKeyValuePair> list, Scanner scanner, String str) {
        scanner.useDelimiter(PARAMETER_SEPARATOR);
        while (scanner.hasNext()) {
            String[] strArrSplit = scanner.next().split(NAME_VALUE_SEPARATOR);
            if (strArrSplit.length == 0 || strArrSplit.length > 2) {
                throw new IllegalArgumentException("bad parameter");
            }
            list.add(new BasicKeyValuePair(decode(strArrSplit[0], str), strArrSplit.length == 2 ? decode(strArrSplit[1], str) : null));
        }
    }

    public static String format(List<BasicKeyValuePair> list, String str) {
        StringBuilder sb = new StringBuilder();
        for (BasicKeyValuePair basicKeyValuePair : list) {
            String strEncode = encode(basicKeyValuePair.first, str);
            String str2 = basicKeyValuePair.second;
            String strEncode2 = str2 != null ? encode(str2, str) : "";
            if (sb.length() > 0) {
                sb.append(PARAMETER_SEPARATOR);
            }
            sb.append(strEncode);
            sb.append(NAME_VALUE_SEPARATOR);
            sb.append(strEncode2);
        }
        return sb.toString();
    }

    private static String decode(String str, String str2) {
        if (str2 == null) {
            str2 = CharEncoding.ISO_8859_1;
        }
        try {
            return URLDecoder.decode(str, str2);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String encode(String str, String str2) {
        if (str2 == null) {
            str2 = CharEncoding.ISO_8859_1;
        }
        try {
            return URLEncoder.encode(str, str2);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}