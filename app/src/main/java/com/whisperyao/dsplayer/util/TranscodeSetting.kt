package com.whisperyao.dsplayer.util

class TranscodeSetting {

    var format: TranscodeFormat = TranscodeFormat.WAV

    var quality: TranscodeQuality = TranscodeQuality.AUTO

    var forceFormats: MutableSet<TranscodeForceFormat> = mutableSetOf()

    fun isFormatMp3(): Boolean = format.isMp3()

    fun isFormatWmv(): Boolean = format.isWmv()

    fun isQualityHigh(): Boolean = quality.isHigh()

    fun isQualityMedium(): Boolean = quality.isMedium()

    fun isQualityLow(): Boolean = quality.isLow()

    fun isQualityAuto(): Boolean = quality.isAuto()

    fun containsForceFormat(format: TranscodeForceFormat?): Boolean {
        return format != null &&
                !format.isAac() &&
                forceFormats.contains(format)
    }

    enum class TranscodeForceFormat {
        AAC,
        FLAC,
        OGG,
        MP3,
        WAV;

        fun isAac() = this == AAC

        fun isFlac() = this == FLAC

        fun isOgg() = this == OGG
    }

    enum class TranscodeQuality {
        HIGH,
        MEDIUM,
        LOW,
        AUTO;

        fun isHigh() = this == HIGH

        fun isMedium() = this == MEDIUM

        fun isLow() = this == LOW

        fun isAuto() = this == AUTO
    }

    enum class TranscodeDownloadQuality {
        ORIGINAL,
        HIGH,
        MEDIUM,
        LOW;

        fun isOriginal() = this == ORIGINAL

        fun isHigh() = this == HIGH

        fun isMedium() = this == MEDIUM

        fun isLow() = this == LOW

        fun getBitrate(): Int {
            return when (this) {
                HIGH -> 320000
                MEDIUM -> 192000
                LOW -> 128000
                ORIGINAL -> 0
            }
        }
    }

    enum class TranscodeFormat {
        MP3,
        WAV;

        fun isMp3() = this == MP3

        fun isWmv() = this == MP3
    }
}