package com.synology.sylib.syhttp3.requestBody;

import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

import kotlin.text.Typography;
import okhttp3.MediaType;
import org.apache.commons.lang3.CharEncoding;


public class SyFormEncodingBuilder {
    private static final MediaType CONTENT_TYPE = MediaType.parse(com.synology.sylib.syhttp3.util.URLEncodedUtil.CONTENT_TYPE);
    private final StringBuilder content = new StringBuilder();

    public SyFormEncodingBuilder add(String str, String str2) throws UnsupportedEncodingException {
        if (this.content.length() > 0) {
            this.content.append(Typography.amp);
        }
        this.content.append(URLEncoder.encode(str, CharEncoding.UTF_8)).append('=').append(URLEncoder.encode(str2, CharEncoding.UTF_8));
        return this;
    }

    public SyFormEncodingBuilder addAll(List<BasicKeyValuePair> list) throws UnsupportedEncodingException {
        if (list != null && !list.isEmpty()) {
            for (BasicKeyValuePair basicKeyValuePair : list) {
                add(basicKeyValuePair.first, basicKeyValuePair.second);
            }
        }
        return this;
    }

    public SyRequestBody build() {
        if (this.content.length() == 0) {
            throw new IllegalStateException("Form encoded body must have at least one part.");
        }
        return SyRequestBody.synoCreate(CONTENT_TYPE, this.content.toString().getBytes(Charset.forName(CharEncoding.UTF_8)));
    }
}
