package com.gtech.kotlinadhar.utils

import java.lang.Exception

/**
 * QrCodeException wraps all the exceptions which occurs while scanning and decoding secure Aadharcard
 * Qr Code
 * @author Raj Deol
 */
class QrCodeException : Exception {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    constructor(cause: Throwable?) : super(cause) {}
}