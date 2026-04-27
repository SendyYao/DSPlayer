package com.whisperyao.dsplayer.util;

import android.content.Context;
import android.text.TextUtils;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.widget.DynamicLyricListAdapter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LyricUtils {

    private static final Pattern TIME_TAG_PATTERN = Pattern.compile("\\[.+?]");
    private static final String LINE_SEPARATOR = "\n";
    private static final String LINE_SEPARATOR_WINDOWS = "\r\n";
    private static final String LINE_SEPARATOR_MAC = "\r";

    /**
     * 提取动态歌词并创建适配器
     * @param strLyric LRC格式的歌词字符串
     * @return DynamicLyricListAdapter 或 null
     */
    public static DynamicLyricListAdapter extractDynamicLyric(String strLyric) {
        if (strLyric == null) {
            return null;
        }

        ArrayList<DynamicLyricListAdapter.LyricItem> arrayList = new ArrayList<>();
        String[] strArrSplit = normalizeLineBreaks(strLyric).split(LINE_SEPARATOR);
        boolean hasLyric = false;
        int offset = 0;

        for (String line : strArrSplit) {
            if (TextUtils.isEmpty(line)) {
                continue;
            }

            ArrayList<DynamicLyricListAdapter.LyricItem> lyricItems = getLyricItems(line, offset);
            if (lyricItems == null) {
                // 尝试提取offset信息
                offset = getOffset(line);
            } else {
                arrayList.addAll(lyricItems);
                hasLyric = true;
            }
        }

        SynoLog.d("LyricUtils", "hasLyric: " + hasLyric);

        return hasLyric ? new DynamicLyricListAdapter(getContext(), arrayList) : null;
    }

    /**
     * 提取纯文本歌词（去除时间标签）
     * @param strLyric LRC格式的歌词字符串
     * @return 纯文本歌词
     */
    public static String extractLyric(String strLyric) {
        if (TextUtils.isEmpty(strLyric)) {
            return null;
        }

        String normalizedLyric = normalizeLineBreaks(strLyric);
        return TIME_TAG_PATTERN.matcher(normalizedLyric).replaceAll("");
    }

    /**
     * 从一行歌词中提取时间标签和歌词文本
     * @param line 单行歌词
     * @param offset 时间偏移量（毫秒）
     * @return LyricItem列表 或 null
     */
    private static ArrayList<DynamicLyricListAdapter.LyricItem> getLyricItems(String line, int offset) {
        // 提取歌词文本（去除所有时间标签）
        String strReplaceAll = TIME_TAG_PATTERN.matcher(line).replaceAll("");

        // 提取所有时间标签
        Matcher matcher = TIME_TAG_PATTERN.matcher(line);
        ArrayList<DynamicLyricListAdapter.LyricItem> lyricItems = null;

        while (matcher.find()) {
            long jFromTimeString = fromTimeString(matcher.group());
            if (jFromTimeString != -1) {
                if (lyricItems == null) {
                    lyricItems = new ArrayList<>();
                }
                lyricItems.add(new DynamicLyricListAdapter.LyricItem(
                        jFromTimeString - offset,
                        strReplaceAll
                ));
            }
        }

        return lyricItems;
    }

    /**
     * 从特殊标记行提取offset值（如 [offset:1500]）
     * @param line 单行歌词
     * @return offset值（毫秒）
     */
    private static int getOffset(String line) {
        if (line != null && line.startsWith("[offset:")) {
            try {
                // 提取 [offset: 和 ] 之间的数字
                int endIndex = line.lastIndexOf("]");
                if (endIndex > 8) {
                    String offsetValue = line.substring(8, endIndex);
                    return Integer.parseInt(offsetValue.trim());
                }
            } catch (NumberFormatException ignored) {
                // 忽略非法的offset值
            }
        }
        return 0;
    }

    /**
     * 将时间标签字符串转换为毫秒值
     * 支持格式：[mm:ss.xx], [mm:ss], [mm:ss:xx]
     * @param strTime 时间标签，如 "[01:23.45]"
     * @return 毫秒值，解析失败返回-1
     */
    private static long fromTimeString(String strTime) {
        if (TextUtils.isEmpty(strTime)) {
            return -1L;
        }

        try {
            // 去除首尾括号，如 [01:23.45] -> 01:23.45]
            int endBracketIndex = strTime.lastIndexOf("]");
            if (endBracketIndex == -1) {
                return -1L;
            }
            String timeContent = strTime.substring(0, endBracketIndex);

            // 提取毫秒部分
            String millisecondsStr = "0";
            String minutesAndSeconds;
            int dotIndex = timeContent.lastIndexOf(".");
            if (dotIndex != -1) {
                millisecondsStr = timeContent.substring(dotIndex + 1);
                minutesAndSeconds = timeContent.substring(0, dotIndex);
            } else {
                minutesAndSeconds = timeContent;
            }

            // 提取分钟和秒
            int colonIndex = minutesAndSeconds.lastIndexOf(":");
            if (colonIndex == -1) {
                return -1L;
            }
            String secondsStr = minutesAndSeconds.substring(colonIndex + 1);
            String minutesStr = minutesAndSeconds.substring(0, colonIndex);

            // 去除开头的 [
            int startBracketIndex = minutesStr.lastIndexOf("[");
            if (startBracketIndex != -1) {
                minutesStr = minutesStr.substring(startBracketIndex + 1);
            }

            // 计算总毫秒
            int minutes = Integer.parseInt(minutesStr);
            int seconds = Integer.parseInt(secondsStr);
            int milliseconds = Integer.parseInt(millisecondsStr);

            return ((long) minutes * 60 + seconds) * 1000 + milliseconds;

        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * 标准化换行符
     */
    private static String normalizeLineBreaks(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(LINE_SEPARATOR_WINDOWS, LINE_SEPARATOR)
                .replace(LINE_SEPARATOR_MAC, LINE_SEPARATOR);
    }

    /**
     * 获取应用上下文
     */
    private static Context getContext() {
        // 根据实际项目调整，可能需要通过Application类或其他方式获取
        return App.getContext();
    }
}
