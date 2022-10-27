package org.xpathqs.framework.log

import org.apache.commons.lang3.time.DurationFormatUtils
import org.xpathqs.log.abstracts.IStreamLog
import org.xpathqs.log.message.IMessage
import java.io.Writer

class SubtitleLogPrinter(
    private val out: Writer
) : IStreamLog {
    private var prevMsg: SubtitleDecorator? = null

    override fun onLog(msg: IMessage) {
        if(prevMsg == null) {
            prevMsg = msg as SubtitleDecorator
        } else {
            prevMsg!!.ts2 = (msg as SubtitleDecorator).ts1
            printCounter()
            printTime(prevMsg!!)
            printBody(prevMsg!!)
            prevMsg = msg
        }

        out.flush()
    }

    private fun printCounter() {
        out.write(SubtitleDecorator.commonData.get().counter++.toString() + "\n")
    }

    private fun printTime(msg: IMessage) {
        val cd = SubtitleDecorator.commonData.get()
        val d = cd.startTs - cd.dockerStart

        var t1 = msg.attributes[SubtitleDecorator.SUB_START] as Long
        if(t1 != 0L) {
            t1 = t1 - cd.startTs + d
        }
        val t2 = msg.attributes[SubtitleDecorator.SUB_END] as Long - cd.startTs + d

        val time = "${t1.toTime()} --> ${t2.toTime()}"
        out.write("$time\n")
    }

    private fun printBody(msg: IMessage) {
        out.write("${msg.bodyMessage.toString().trim()}\n\n")
    }

    private fun Long.toTime(): String {
        return DurationFormatUtils.formatDuration(this, "HH:mm:ss,SSS")
    }
}