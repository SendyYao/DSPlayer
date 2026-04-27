package com.whisperyao.dsplayer.playing;


import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.util.Log;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.DeviceCustomization;
import com.whisperyao.dsplayer.util.ObjFile;
import com.whisperyao.dsplayer.vos.EqualizerTypeDataVo;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class EqualizerSettings {
    public static final int DEFAUL_BANDLEVEL = 0;
    public static final short DEFAUL_INDEX = 0;
    public static final boolean DEFAUL_USE_TMP_EQUALIZER = false;
    public static final String EMPTY_STRING = "";
    private static final String EQUALIZER_CUSTOM_INFO = "equalizer_custom_info";
    private static final String LOG = "EqualizerSettings";
    private static final int SEEKBAR_INTERVAL = 100;
    private static EqualizerSettings mInstance;
    private short mBandNums;
    private int[] mCenterFreqs;
    private int mSeekBarMax;
    private EqualizerTypeDataVo.EqualizerType mTmpEqualizerType;
    private short maxEQLevel;
    private short minEQLevel;
    private final ArrayList<Callback> callbacks = new ArrayList<>();
    private HashMap<String, Short> mNameMaps = new HashMap<>();
    private short mPresetNums = 0;
    private boolean mEqualizerInit = false;
    private boolean mEnableEqualizer = false;
    private boolean mUseTmpEqualizer = false;
    List<EqualizerTypeDataVo.EqualizerType> types = new ArrayList();
    private int mSelectedEqualizerIndex = 0;

    public interface Callback {
        void updateEqualizer();
    }

    public void addCallback(Callback callback) {
        this.callbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.callbacks.remove(callback);
    }

    public void notifyUpdateEqualizer() {
        Iterator<Callback> it = this.callbacks.iterator();
        while (it.hasNext()) {
            it.next().updateEqualizer();
        }
    }

    public static EqualizerSettings getInstance() {
        if (mInstance == null) {
            mInstance = new EqualizerSettings();
        }
        return mInstance;
    }

    private EqualizerSettings() {
        init();
    }

    public boolean init() {
        if (!DeviceCustomization.supportAudioEffect()) {
            Log.w(LOG, "This device doesn't support AudioEffect");
            mEqualizerInit = false;
            return false;
        }

        MediaPlayer mediaPlayer = null;
        Equalizer equalizer = null;

        try {
            mediaPlayer = new MediaPlayer();

            equalizer = new Equalizer(
                    0,
                    mediaPlayer.getAudioSessionId()
            );

            // 清空已有数据
            types.clear();
            mNameMaps.clear();

            // 初始化临时自定义 EQ
            mTmpEqualizerType = new EqualizerTypeDataVo.EqualizerType(
                    "tmp",
                    EqualizerTypeDataVo.EqualizerSourceType.Tmp,
                    getEmptyBandLevels()
            );

            // 是否启用 EQ
            mEnableEqualizer = AudioPreference.enableEqualizer();

            // 系统预设数量
            mPresetNums = equalizer.getNumberOfPresets();

            // 加载系统预设
            for (short i = 0; i < mPresetNums; i++) {
                String presetName = equalizer.getPresetName(i);

                addEqualizerType(
                        i,
                        presetName,
                        EqualizerTypeDataVo.EqualizerSourceType.Default,
                        null
                );
            }

            // 加载用户自定义预设
            loadCustomEqualizer();

            // band 数量
            mBandNums = equalizer.getNumberOfBands();

            // EQ 范围
            short[] levelRange = equalizer.getBandLevelRange();

            minEQLevel = levelRange[0];
            maxEQLevel = levelRange[1];

            // SeekBar 最大值
            mSeekBarMax = (maxEQLevel - minEQLevel) / 100;

            // 中心频率
            mCenterFreqs = new int[mBandNums];

            for (short i = 0; i < mBandNums; i++) {
                mCenterFreqs[i] = equalizer.getCenterFreq(i);
            }

            mEqualizerInit = true;

        } catch (Exception e) {
            Log.e(LOG, "Equalizer failed. " + e);
            mEqualizerInit = false;

        } finally {
            if (equalizer != null) {
                equalizer.release();
            }

            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        }

        return mEqualizerInit;
    }

    public void updateEqualizer(Equalizer equalizer) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        EqualizerTypeDataVo.EqualizerType currentEqualizerType;
        equalizer.setEnabled(this.mEnableEqualizer);
        if (this.mEnableEqualizer) {
            if (this.mUseTmpEqualizer) {
                currentEqualizerType = this.mTmpEqualizerType;
            } else {
                currentEqualizerType = getCurrentEqualizerType();
            }
            if (currentEqualizerType.isCustom() || currentEqualizerType.isTmp()) {
                short[] bandLevels = currentEqualizerType.getBandLevels();
                for (short s = 0; s < bandLevels.length; s = (short) (s + 1)) {
                    equalizer.setBandLevel(s, checkBandlevel(bandLevels[s]));
                }
                return;
            }
            String name = currentEqualizerType.getName();
            if (containsName(name)) {
                equalizer.usePreset(getNameValue(name));
            } else {
                equalizer.usePreset((short) 0);
            }
        }
    }

    public boolean isEqualizerInit() {
        return this.mEqualizerInit;
    }

    public boolean enableEqualizer() {
        return this.mEnableEqualizer && this.mEqualizerInit;
    }

    public void setEnableEqualizer(boolean b) {
        this.mEnableEqualizer = b;
    }

    public void setUseTmpEqualizer(boolean b) {
        this.mUseTmpEqualizer = b;
    }

    public int getSelectedEqualizerIndex() {
        return this.mSelectedEqualizerIndex;
    }

    public void setSelectedEqualizerIndex(int index) {
        this.mSelectedEqualizerIndex = index;
    }

    public int getCustomNums() {
        int size = this.types.size() - this.mPresetNums;
        return Math.max(size, 0);
    }

    public short getBandNums() {
        return this.mBandNums;
    }

    public int getSeekBarMax() {
        return this.mSeekBarMax;
    }

    public int[] getCenterFreqs() {
        return this.mCenterFreqs;
    }

    private short checkBandlevel(short bandlevel) {
        short s = this.minEQLevel;
        if (bandlevel < s) {
            return s;
        }
        short s2 = this.maxEQLevel;
        return bandlevel > s2 ? s2 : bandlevel;
    }

    private static File getEqualizerCustomInfoFile() {
        return new File(Common.getDSaudioAppFolder() + EQUALIZER_CUSTOM_INFO);
    }

    public void saveCustomEqualizer() {
        EqualizerTypeDataVo equalizerTypeDataVo = new EqualizerTypeDataVo();
        equalizerTypeDataVo.setTypes(getCustomsTypes());
        ObjFile.saveObjectToFile(equalizerTypeDataVo, getEqualizerCustomInfoFile(), EqualizerTypeDataVo.class);
    }

    public void loadCustomEqualizer() {
        List<EqualizerTypeDataVo.EqualizerType> types;
        EqualizerTypeDataVo equalizerTypeDataVo = (EqualizerTypeDataVo) ObjFile.getObjectFromFile(getEqualizerCustomInfoFile(), EqualizerTypeDataVo.class);
        if (equalizerTypeDataVo == null || (types = equalizerTypeDataVo.getTypes()) == null) {
            return;
        }
        for (EqualizerTypeDataVo.EqualizerType equalizerType : types) {
            addEqualizerType((short) 0, equalizerType.getName(), EqualizerTypeDataVo.EqualizerSourceType.Custom, equalizerType.getBandLevels());
        }
    }

    private List<EqualizerTypeDataVo.EqualizerType> getCustomsTypes() {
        ArrayList<EqualizerTypeDataVo.EqualizerType> arrayList = new ArrayList<>();
        for (EqualizerTypeDataVo.EqualizerType equalizerType : this.types) {
            if (equalizerType.isCustom()) {
                arrayList.add(equalizerType);
            }
        }
        return arrayList;
    }

    public EqualizerTypeDataVo.EqualizerType getCurrentEqualizerType() {
        String equalizerType = AudioPreference.getEqualizerType();
        for (EqualizerTypeDataVo.EqualizerType equalizerType2 : this.types) {
            if (equalizerType2.getName().equals(equalizerType)) {
                return equalizerType2;
            }
        }
        return this.types.get(0);
    }

    public boolean setCurrentEqualizerType(int pos) {
        boolean z;
        if (pos < 0 || pos >= this.types.size()) {
            z = false;
        } else {
            AudioPreference.setEqualizerType(this.types.get(pos).getName());
            z = true;
        }
        if (!z) {
            Log.w(LOG, " warning: the equalizer index " + pos + " is not exist");
        }
        return z;
    }

    public boolean setCurrentEqualizerType(String name) {
        Iterator<EqualizerTypeDataVo.EqualizerType> it = this.types.iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (it.next().getName().equals(name)) {
                AudioPreference.setEqualizerType(name);
                z = true;
            }
        }
        if (!z) {
            Log.w(LOG, " warning: the equalizer type " + name + " is not exist");
        }
        return z;
    }

    public boolean containsName(String name) {
        return this.mNameMaps.containsKey(name);
    }

    public short getNameValue(String name) {
        return this.mNameMaps.get(name).shortValue();
    }

    public short progressToBandLevel(int progress) {
        return (short) ((progress - (this.mSeekBarMax / 2)) * 100);
    }

    public int bandLevelToProgress(short bandlevel) {
        return (bandlevel / 100) + (this.mSeekBarMax / 2);
    }

    public String getBandLevelValueText(short level) {
        return (level / 100) + "db";
    }

    public String getCenterFreqText(int freq) {
        String str;
        int i = freq / 1000;
        if (i < 1000) {
            str = "";
        } else {
            i /= 1000;
            str = "k";
        }
        return i + str + " Hz";
    }

    public boolean addEqualizerType(short i, String name, EqualizerTypeDataVo.EqualizerSourceType type, short[] bandLevels) {
        if (this.mNameMaps.containsKey(name)) {
            return false;
        }
        this.mNameMaps.put(name, i);
        this.types.add(new EqualizerTypeDataVo.EqualizerType(name, type, bandLevels));
        return true;
    }

    public boolean editEqualizerType(String originName, String name, short[] bandLevels) {
        if (!originName.equals(name) && this.mNameMaps.containsKey(name)) {
            return false;
        }
        this.mNameMaps.remove(originName);
        this.mNameMaps.put(name, (short) 0);
        for (EqualizerTypeDataVo.EqualizerType equalizerType : this.types) {
            if (equalizerType.getName().equals(originName)) {
                equalizerType.setName(name);
                equalizerType.setBandLevels(bandLevels);
                return true;
            }
        }
        return true;
    }

    public boolean removeEqualizerType(String name) {
        if (!this.mNameMaps.containsKey(name)) {
            return false;
        }
        this.mNameMaps.remove(name);
        for (EqualizerTypeDataVo.EqualizerType equalizerType : this.types) {
            if (equalizerType.getName().equals(name)) {
                this.types.remove(equalizerType);
                return true;
            }
        }
        return true;
    }

    public int getEqualizerTypeNum() {
        return this.types.size();
    }

    public final EqualizerTypeDataVo.EqualizerType getEqualizerType(int pos) {
        if (pos >= 0 && pos < this.types.size()) {
            return this.types.get(pos);
        }
        return this.types.get(0);
    }

    public int getEqualizerTypeIndex(String name) {
        for (int i = 0; i < this.types.size(); i++) {
            if (this.types.get(i).getName().equals(name)) {
                return i;
            }
        }
        return 0;
    }

    public short[] getEmptyBandLevels() {
        short[] sArr = new short[this.mBandNums];
        for (short s = 0; s < this.mBandNums; s = (short) (s + 1)) {
            sArr[s] = 0;
        }
        return sArr;
    }

    public boolean setTmpEqualizerBandLevels(final short[] bandlevels) {
        short[] emptyBandLevels;
        int length = bandlevels.length;
        int i = this.mBandNums;
        if (length == i) {
            emptyBandLevels = new short[i];
            for (short s = 0; s < this.mBandNums; s = (short) (s + 1)) {
                emptyBandLevels[s] = bandlevels[s];
            }
        } else {
            Log.w(LOG, " warning: the tmp bandlevels length " + bandlevels.length + " not equal to bandNums " + ((int) this.mBandNums));
            emptyBandLevels = getEmptyBandLevels();
            if (bandlevels.length < this.mBandNums) {
                for (short s2 = 0; s2 < bandlevels.length; s2 = (short) (s2 + 1)) {
                    emptyBandLevels[s2] = bandlevels[s2];
                }
            } else {
                for (short s3 = 0; s3 < this.mBandNums; s3 = (short) (s3 + 1)) {
                    emptyBandLevels[s3] = bandlevels[s3];
                }
            }
        }
        for (short s4 = 0; s4 < emptyBandLevels.length; s4 = (short) (s4 + 1)) {
            emptyBandLevels[s4] = checkBandlevel(emptyBandLevels[s4]);
        }
        this.mTmpEqualizerType.setBandLevels(emptyBandLevels);
        return true;
    }

    public short[] getTmpEqualizerBandLevels() {
        short[] sArr = new short[this.mBandNums];
        short[] bandLevels = this.mTmpEqualizerType.getBandLevels();
        for (short s = 0; s < this.mBandNums; s = (short) (s + 1)) {
            sArr[s] = bandLevels[s];
        }
        return sArr;
    }
}