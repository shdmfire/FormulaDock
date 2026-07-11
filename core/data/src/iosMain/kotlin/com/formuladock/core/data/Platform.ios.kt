package com.formuladock.core.data

actual fun platform() = "iOS"
actual fun currentTimeMillis(): Long = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()