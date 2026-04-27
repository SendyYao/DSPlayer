package com.whisperyao.dsplayer.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.item.DrawerItem;
import java.util.List;

public class DrawerAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<DrawerItem> mDrawerItems;
    private final LayoutInflater mInflater;
    private int mSelectedPos;

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public DrawerAdapter(Context context, List<DrawerItem> drawerItems) {
        this.mContext = context;
        this.mDrawerItems = drawerItems;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    @Override
    public int getItemViewType(int position) {
        return this.mDrawerItems.get(position).getItemType();
    }

    @Override
    public int getCount() {
        return this.mDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mDrawerItems.get(position);
    }

    public int getItemPosition(int itemId) {
        for (int i = 0; i < this.mDrawerItems.size(); i++) {
            if (this.mDrawerItems.get(i).getItemId() == itemId) {
                return i;
            }
        }
        return -1;
    }

    public void setSelectedItem(int position) {
        this.mSelectedPos = position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) throws Resources.NotFoundException {
        ViewHolder viewHolder;
        int i;
        DrawerItem drawerItem = this.mDrawerItems.get(position);
        if (convertView == null) {
            if (drawerItem.getItemType() == 0) {
                i = R.layout.drawer_list_item;
            } else {
                i = drawerItem.getItemType() == 1 ? R.layout.drawer_list_setting_item : 0;
            }
            convertView = this.mInflater.inflate(i, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.icon = convertView.findViewById(R.id.icon);
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.content = convertView.findViewById(R.id.content);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (drawerItem.getItemType() == 0) {
            viewHolder.title.setText(drawerItem.getTitle());
            Drawable drawable = ResourcesCompat.getDrawable(this.mContext.getResources(), drawerItem.getIconRes(), null);
            if (drawable != null) {
                Drawable drawableMutate = drawable.mutate();
                viewHolder.title.setCompoundDrawablesWithIntrinsicBounds(drawableMutate, null, null, null);
                if (this.mSelectedPos == position) {
                    viewHolder.title.setTextColor(Color.WHITE);
                    drawableMutate.setAlpha(255);
                } else {
                    int semiTransparentWhite = Color.argb(128, 255, 255, 255);
                    viewHolder.title.setTextColor(semiTransparentWhite);
                    drawableMutate.setAlpha(128);
                }
            }
        } else if (drawerItem.getItemType() == 1) {
            if (Common.isLogin()) {
                viewHolder.title.setText(drawerItem.getTitle());
                viewHolder.content.setText(drawerItem.getContent());
                viewHolder.content.setVisibility(View.VISIBLE);
            } else {
                viewHolder.title.setText(R.string.settings);
                viewHolder.content.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    public static class ViewHolder {
        TextView content;
        ImageView icon;
        TextView title;

        public ViewHolder() {
        }
    }
}