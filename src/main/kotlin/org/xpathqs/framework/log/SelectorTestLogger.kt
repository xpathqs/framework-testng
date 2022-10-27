package org.xpathqs.framework.log


import org.xpathqs.log.BaseLogger
import org.xpathqs.log.Logger
import org.xpathqs.log.printers.StreamLogPrinter
import org.xpathqs.log.printers.args.NoArgsProcessor
import org.xpathqs.log.printers.args.StyleArgsProcessor
import org.xpathqs.log.printers.args.TimeArgsProcessor
import org.xpathqs.log.printers.body.BodyProcessorImpl
import org.xpathqs.log.printers.body.HierarchyBodyProcessor
import org.xpathqs.log.printers.body.StyledBodyProcessor
import org.xpathqs.log.restrictions.RestrictionRuleHard
import org.xpathqs.log.restrictions.value.ExcludeTags
import org.xpathqs.log.restrictions.value.LogLevelLessThan
import org.xpathqs.log.style.Style
import org.xpathqs.log.style.StyledString.Companion.defaultStyles

object SelectorTestLogger: SelectorTestLoggerCls()
open class SelectorTestLoggerCls(
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
                    level1 = Style(textColor = 48),
                    level2 = Style(textColor = 40),
                    level3 = Style(textColor = 35)
                ),
            writer = System.out
        )
    ),
    protected var allureLog: Logger = Logger(
        restrictions =
            listOf(
                RestrictionRuleHard(
                    LogLevelLessThan(1),
                ),
                RestrictionRuleHard(
                    ExcludeTags("info", "trace", "debug")
                )
            ),
        notifiers =
        arrayListOf(
            AllureLogCallback()
        )
    ),
    protected var videoSubLog: Logger = Logger(),
    protected var fileLog: Logger =
        Logger(streamPrinter =
            ThreadFilePrinter()
        )
): BaseLogger(
    arrayListOf(
        consoleLog,
        allureLog,
        fileLog
    ),
    defaultStyles
)