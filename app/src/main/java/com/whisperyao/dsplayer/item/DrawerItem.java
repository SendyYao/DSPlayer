package com.whisperyao.dsplayer.item;

/* loaded from: classes2.dex */
public class DrawerItem {
    public static final int TYPE_COUNT = 2;
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_SETTING = 1;
    String content;
    int iconRes;
    int itemId;
    int itemType;
    String title;

    public static DrawerItem createNormalItem(int iconRes, String title, int itemId) {
        DrawerItem drawerItem = new DrawerItem();
        drawerItem.itemType = 0;
        drawerItem.iconRes = iconRes;
        drawerItem.title = title;
        drawerItem.itemId = itemId;
        return drawerItem;
    }

    public static DrawerItem createSettingItem(int iconRes, String title, String content, int itemId) {
        DrawerItem drawerItem = new DrawerItem();
        drawerItem.itemType = 1;
        drawerItem.iconRes = iconRes;
        drawerItem.title = title;
        drawerItem.content = content;
        drawerItem.itemId = itemId;
        return drawerItem;
    }

    public int getIconRes() {
        return this.iconRes;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public int getItemType() {
        return this.itemType;
    }

    public int getItemId() {
        return this.itemId;
    }
}