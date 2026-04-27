package com.synology.sylib.security.migrate.data;

/* loaded from: classes.dex */
public class KsRef<T> {
    private Class<T> clazz;
    private T value;

    public static KsRef<String> newString() {
        return create(String.class, null);
    }

    public static KsRef<Boolean> newBoolean() {
        return create(Boolean.class, false);
    }

    public static <T> KsRef<T> create(Class<T> cls) {
        return create(cls, null);
    }

    public static <T> KsRef<T> create(Class<T> cls, T t) {
        return new KsRef<>(cls, t);
    }

    private KsRef(Class<T> cls, T t) {
        this.value = t;
        this.clazz = cls;
    }

    public void set(T t) {
        this.value = t;
    }

    public T get() {
        return this.value;
    }

    public boolean isNull() {
        return this.value == null;
    }

    public Class getTypeClass() {
        return this.clazz;
    }
}