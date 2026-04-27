package com.synology.sylibx.synofile

import java.io.File
import kotlin.io.FileSystemException

class TerminateException(file: File) : FileSystemException(
    file = file,
    other = null,
    reason = null
)