package com.synology.sylib.syhttp3.relay.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.synology.sylib.syhttp3.exceptions.UnexpectedJsonException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/* loaded from: classes2.dex */
public class SafeGson {
    private final Gson gson;

    public SafeGson() {
        this(new Gson());
    }

    public SafeGson(Gson gson) {
        this.gson = gson;
    }

    public String toJson(JsonElement jsonElement) {
        return this.gson.toJson(jsonElement);
    }

    public <T> T fromJson(InputStream inputStream, Type type, Charset charset) throws UnexpectedJsonException {
        return (T) fromJson(new InputStreamReader(inputStream, charset), type);
    }

    public <T> T fromJson(Reader reader, Type type) throws UnexpectedJsonException {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line);
                } else {
                    return (T) this.gson.fromJson(sb.toString(), type);
                }
            }
        } catch (JsonSyntaxException e) {
            throw new UnexpectedJsonException(sb.toString(), e);
        } catch (Throwable th) {
            throw new UnexpectedJsonException("", th);
        }
    }
}