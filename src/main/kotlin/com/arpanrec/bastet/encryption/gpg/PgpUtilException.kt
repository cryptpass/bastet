package com.arpanrec.bastet.encryption.gpg

class PgpUtilException : Exception {
    constructor(message: String?) : super(message)

    internal constructor(message: String?, t: Throwable?) : super(message, t)
}