package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;
import java.util.Collections;


public class DynamicLyricListAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<LyricItem> mLyricList;
    private int mSelectPosition = -1;

    @Override
    public long getItemId(int position) {
        return position;
    }

    public DynamicLyricListAdapter(Context context, ArrayList<LyricItem> lyricList) {
        this.mContext = null;
        this.mLyricList = new ArrayList<>();
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mLyricList = lyricList;
        Collections.sort(lyricList, (lhs, rhs) -> (int) (lhs.getTime() - rhs.getTime()));
    }

    public ArrayList<LyricItem> getLyricList() {
        return this.mLyricList;
    }

    public ArrayList<Long> getTimeList() {
        ArrayList<Long> arrayList = new ArrayList<>();
        for (LyricItem lyricItem : this.mLyricList) {
            arrayList.add(lyricItem.getTime());
        }
        return arrayList;
    }

    public void setSelectPosition(int pos) {
        // SynoLog.i("DynamicLyricListAdapter", "setSelectPosition: " + pos);
        if (this.mSelectPosition != pos) {
            this.mSelectPosition = pos;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return this.mLyricList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mLyricList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) this.mInflater.inflate(R.layout.dynamic_lyric_item, null);
        textView.setText(this.mLyricList.get(position).getLine());
        if (this.mSelectPosition == position) {
            textView.setTextColor(Color.GREEN);
        } else {
            textView.setTextColor(Color.WHITE);
        }
        return textView;
    }

    public static class LyricItem {
        private final String line;
        private final long time;

        public LyricItem(long time, String line) {
            this.time = time;
            this.line = line;
        }

        public long getTime() {
            return this.time;
        }

        public String getLine() {
            return this.line;
        }
    }
}
