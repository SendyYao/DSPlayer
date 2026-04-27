package com.synology.sylibx.synofile

enum class GrantStatus(
    val value: String
) {
    Granted("granted"),
    NeedManager("manager"),
    NeedSAF("saf"),
    NeedSAFForSD("saf_sd"),
    NeedStorage("storage"),
    Invalid("invalid"),
    PickerUnAvailable("picker_unavailable");

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromValue(value: String?): GrantStatus {
            return map[value] ?: Granted
        }
    }

    val isGranted: Boolean
        get() = this == Granted

    val isNeedSAF: Boolean
        get() = this == NeedSAF || this == NeedSAFForSD

    val isNeedStorage: Boolean
        get() = this == NeedStorage

    val isNeedManager: Boolean
        get() = this == NeedManager

    val isNeedRequest: Boolean
        get() = isNeedSAF || isNeedStorage || isNeedManager

    val isPickerUnAvailable: Boolean
        get() = this == PickerUnAvailable
}