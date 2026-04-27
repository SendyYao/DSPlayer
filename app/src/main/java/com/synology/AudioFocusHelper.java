package com.synology;

import android.content.Context;
import android.media.AudioManager;

public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
    AudioManager mAM;
    MusicFocusable mFocusable;

    public interface MusicFocusable {
        void onGainedAudioFocus();

        void onLostAudioFocus();

        void onLostAudioFocusCanDuck();
    }

    public AudioFocusHelper(Context ctx, MusicFocusable focusable) {
        this.mAM = (AudioManager) ctx.getSystemService("audio");
        this.mFocusable = focusable;
    }

    public boolean requestFocus() {
        return 1 == this.mAM.requestAudioFocus(this, 3, 1);
    }

    public boolean abandonFocus() {
        return 1 == this.mAM.abandonAudioFocus(this);
    }

    @Override // android.media.AudioManager.OnAudioFocusChangeListener
    public void onAudioFocusChange(int focusChange) {
        MusicFocusable musicFocusable = this.mFocusable;
        if (musicFocusable == null) {
            return;
        }
        if (focusChange == -3) {
            musicFocusable.onLostAudioFocusCanDuck();
            return;
        }
        if (focusChange == -2 || focusChange == -1) {
            musicFocusable.onLostAudioFocus();
        } else {
            if (focusChange != 1) {
                return;
            }
            musicFocusable.onGainedAudioFocus();
        }
    }
}
