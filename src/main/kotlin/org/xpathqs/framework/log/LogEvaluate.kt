package org.xpathqs.framework.log

import org.xpathqs.gwt.GIVEN
import org.xpathqs.gwt.ILogEvaluate
import org.xpathqs.gwt.When
import org.xpathqs.log.style.StyledString
import org.xpathqs.web.selenium.executor.SeleniumBaseExecutor.Companion.enableScreenshots

class LogEvaluate : ILogEvaluate {
    override fun <G : Any> GIVEN(msg: StyledString, obj: GIVEN<G>, f: () -> G) {
        obj.given = if (msg.toString().isNotEmpty()) {
            runAndEnableScreenshots(config.actionInGiven) {
                GIVEN.log.action(StyledString("ДАНО ") + msg, GIVEN.GIVEN, f)
            }
        } else {
            f()
        }
    }

    override fun <G : Any, W> WHEN(msg: StyledString, obj: GIVEN<G>, f: GIVEN<G>.() -> W): W {
        return GIVEN.log.action(StyledString("КОГДА ") + msg, GIVEN.WHEN) {
            runAndEnableScreenshots(config.actionInWhen) {
                obj.f()
            }
        }
    }

    override fun <G : Any, W> THEN(msg: StyledString, obj: When<G, W>, f: When<G, W>.() -> Unit) {
        GIVEN.log.action(StyledString("ТОГДА ") + msg, GIVEN.THEN) {
            runAndEnableScreenshots(config.actionInThen) {
                obj.f()
            }
        }
    }

    private fun<T> runAndEnableScreenshots(cond: Boolean, f: ()->T): T {
        val prev = enableScreenshots
        if(cond) {
            enableScreenshots = true
        }

        val res = f()

        enableScreenshots = prev
        return res
    }

    private val config: GWTConfigData
        get() = AllureLogCallback.config.get()
}