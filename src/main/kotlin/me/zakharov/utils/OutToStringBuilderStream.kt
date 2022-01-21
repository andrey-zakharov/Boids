package me.zakharov.utils

import java.io.OutputStream
class OutToStringBuilderStream : OutputStream() {
    private val sb = StringBuilder()
    override fun write(b: Int) {
        sb.append(b.toChar())
    }

    val toStr get() = sb.toString()
}
