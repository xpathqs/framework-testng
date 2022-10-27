package org.xpathqs.framework.log

import org.xpathqs.log.message.IMessage
import org.xpathqs.log.message.LogAttributes
import org.xpathqs.log.message.MessageDecorator

class CommonData {
    var dockerStart = 0L
    var startTs: Long = 0
    var prevTs: Long = 0
    var counter: Long = 1
}

class SubtitleDecorator(origin: IMessage) : MessageDecorator(origin) {
    var ts1 = System.currentTimeMillis()
    var ts2 = System.currentTimeMillis()

    override val selfAttributes: LogAttributes
        get() = hashMapOf(
            SUB_START to ts1,
            SUB_END to ts2
        )

    init {
        val cd = commonData.get()
        if(cd != null) {
            if(cd.startTs == 0L) {
                cd.startTs = ts2
                ts1 = 0
                cd.prevTs = ts2
            } else {
                ts1 = cd.prevTs
                cd.prevTs = ts2
            }
        }
    }

    companion object {
        val commonData = ThreadLocal<CommonData>()
        const val SUB_START = "SUB_START"
        const val SUB_END = "SUB_END"
    }
}