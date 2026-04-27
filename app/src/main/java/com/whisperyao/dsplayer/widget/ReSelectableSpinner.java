package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import androidx.appcompat.widget.AppCompatSpinner;

public class ReSelectableSpinner extends AppCompatSpinner {
    private boolean initializedView;
    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;
    private int mPosition;

    public ReSelectableSpinner(Context context) {
        super(context);
        this.initializedView = false;
        this.mOnItemSelectedListener = null;
        this.mPosition = -1;
    }

    public ReSelectableSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initializedView = false;
        this.mOnItemSelectedListener = null;
        this.mPosition = -1;
    }

    public ReSelectableSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initializedView = false;
        this.mOnItemSelectedListener = null;
        this.mPosition = -1;
    }

    @Override // android.widget.AdapterView
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        this.mOnItemSelectedListener = listener;
        super.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                ReSelectableSpinner.this.mPosition = position;
                if (!ReSelectableSpinner.this.initializedView) {
                    ReSelectableSpinner.this.initializedView = true;
                } else {
                    ReSelectableSpinner.this.mOnItemSelectedListener.onItemSelected(adapterView, view, position, id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                ReSelectableSpinner.this.mOnItemSelectedListener.onNothingSelected(adapterView);
            }
        });
    }

    @Override // android.widget.AbsSpinner, android.widget.AdapterView
    public void setSelection(int position) {
        AdapterView.OnItemSelectedListener onItemSelectedListener;
        super.setSelection(position);
        if (this.mPosition == position && (onItemSelectedListener = this.mOnItemSelectedListener) != null) {
            onItemSelectedListener.onItemSelected(this, getSelectedView(), this.mPosition, getSelectedItemId());
        }
        this.mPosition = position;
    }
}