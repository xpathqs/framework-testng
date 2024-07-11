package org.xpathqs.framework.log


import org.xpathqs.log.Log
import org.xpathqs.log.BaseLogger
import org.xpathqs.log.Logger
import org.xpathqs.log.MessageProcessor
import org.xpathqs.log.abstracts.IArgsProcessor
import org.xpathqs.log.message.IMessage

import org.xpathqs.log.message.MessageDecorator
import org.xpathqs.log.message.TextMessage
import org.xpathqs.log.printers.StreamLogPrinter
import org.xpathqs.log.printers.args.NoArgsProcessor
import org.xpathqs.log.printers.args.StyleArgsProcessor
import org.xpathqs.log.printers.args.TimeArgsProcessor
import org.xpathqs.log.printers.body.BodyProcessorImpl
import org.xpathqs.log.printers.body.HierarchyBodyProcessor
import org.xpathqs.log.printers.body.StyledBodyProcessor
import org.xpathqs.log.restrictions.RestrictionRuleHard
import org.xpathqs.log.restrictions.source.ExcludeByRootMethodClsSimple
import org.xpathqs.log.restrictions.value.ExcludeTags
import org.xpathqs.log.restrictions.value.IncludeTags
import org.xpathqs.log.restrictions.value.LogLevelLessThan
import org.xpathqs.log.style.Style
import org.xpathqs.log.style.StyledString
import org.xpathqs.log.style.StyledString.Companion.defaultStyles
import java.io.File
import java.io.OutputStreamWriter


object UiLogger: UiLoggerCls()
open class UiLoggerCls(
    protected var consoleLog: Logger = Logger(
        streamPrinter = StreamLogPrinter(
            argsProcessor =
            StyleArgsProcessor(
                TimeArgsProcessor(
                    NoArgsProcessor()
                ),
                Style(textColor = 60)
            ),
            bodyProcessor =
            StyledBodyProcessor(
                HierarchyBodyProcessor(
                    BodyProcessorImpl()
                ),
                /*level1 = Style(textColor = 48),
                level2 = Style(textColor = 40),
                level3 = Style(textColor = 35)*/
            ),
            writer = System.out
        ),
        restrictions = listOf(
            RestrictionRuleHard(
                source = ExcludeByRootMethodClsSimple("IBaseModel", "findParent")
            ),
 /*           RestrictionRuleHard(
                source = ExcludeByRootMethodClsSimple("Navigator", "getCurrentPage")
            ),*/
        )

    ),
    protected var allureLog: Logger = Logger(
        restrictions =
        listOf(
            RestrictionRuleHard(
                IncludeTags("step", "GIVEN", "WHEN", "THEN")
            ),
            RestrictionRuleHard(
                source = ExcludeByRootMethodClsSimple(method = "closeModal")
            )
        ),
        notifiers =
        arrayListOf(
            AllureLogCallback()
        ),
        //   name = "allure"
    ),
    protected var videoSubLog: Logger = Logger()
): BaseLogger(
    arrayListOf(
        consoleLog,
        allureLog,
        videoSubLog
    ),
    defaultStyles
) {
    init {
        MessageProcessor.consoleLog.set(consoleLog)
    }

    override fun decorateMessage(msg: MessageDecorator): MessageDecorator {
        return SubtitleDecorator(msg)
    }

    fun updatePath(path: String) {
        val f = File("$path/video.srt")

        loggers.remove(videoSubLog)

        videoSubLog =
            Logger(
                streamPrinter =
                SubtitleLogPrinter(
                    OutputStreamWriter(
                        File("$path/video.srt").outputStream()
                    )
                ),
                restrictions =
                listOf(RestrictionRuleHard(
                    LogLevelLessThan(1)
                ))
            )

        loggers.add(videoSubLog)
    }

    fun flush() {
        /*if(commonData.get().useDocker) {
            videoSubLog.log(
                SubtitleDecorator(
                    TextMessage("")
                )
            )
            SubtitleDecorator.commonData.get().startTs = 0
            SubtitleDecorator.commonData.get().counter = 1
        }*/
    }
}

fun noLog(action: ()->Unit) {
    Log.step(listOf(
        RestrictionRuleHard(
            rule = ExcludeTags("step")
        )
    )) {
        action()
    }
}

fun step(msg: String, action: ()->Unit) = step(StyledString(msg), action)
fun step(msg: StyledString, action: ()->Unit) {
    Log.step(msg, listOf(
        RestrictionRuleHard(
            rule = ExcludeTags("step")
        )
    )) {
        action()
    }
}