package com.synology.sylibx.synofile

enum class GrantResult(
    val code: Int
) {
    Granted(5001),
    Denied(5002),
    NeverAsk(5003),
    Cancel(5004);

    companion object {
        private val codeMap = entries.associateBy { it.code }

        @JvmStatic
        fun fromCode(code: Int): GrantResult? {
            return codeMap[code]
        }

        @JvmStatic
        fun isGranted(code: Int): Boolean {
            return fromCode(code) == Granted
        }

        @JvmStatic
        fun isDenied(code: Int): Boolean {
            return fromCode(code) == Denied
        }

        @JvmStatic
        fun isNeverAsk(code: Int): Boolean {
            return fromCode(code) == NeverAsk
        }

        @JvmStatic
        fun isCancel(code: Int): Boolean {
            return fromCode(code) == Cancel
        }
    }
}