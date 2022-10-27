package org.xpathqs.framework.pom

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.fail
import io.qameta.allure.Allure
import org.testng.SkipException
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.base.findAnnotation
import org.xpathqs.core.selector.base.hasAnnotation
import org.xpathqs.core.selector.base.hasAnyParentAnnotation
import org.xpathqs.core.selector.block.findWithAnnotation
import org.xpathqs.core.selector.extensions.rootParent
import org.xpathqs.driver.extensions.*
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.framework.base.BaseUiTest
import org.xpathqs.gwt.GIVEN
import org.xpathqs.log.style.StyleFactory
import org.xpathqs.web.selenium.executor.SeleniumBaseExecutor
import java.time.Duration
import kotlin.time.measureTime

interface ISelectorCheck {
    fun checkSelector(sel: BaseSelector)
}

class SelectorCheck(
) : ISelectorCheck {

    override fun checkSelector(sel: BaseSelector) {
        Allure.getLifecycle().updateTestCase {
            it.name = sel.name
        }

        Log.tag(
            StyleFactory.testTitle("                    ${sel.name}                    "), "title"
        )

        Log.action("Selector ${sel.name}", GIVEN.WHEN) {
            try {
                sel.makeVisible()
            } catch (e: SkipException) {
                throw e
            }
            catch (e: Exception) {
                if(sel.hasAnnotation(UI.Visibility.Backend::class) || sel.hasAnyParentAnnotation(UI.Visibility.Backend::class)) {
                    throw SkipException("Селектор не может быть проверен, так как")
                }
                e.printStackTrace()
                fail(e.message ?: "")
            }
        }
        Log.action("Должен быть видимым", GIVEN.THEN) {
            sel.findAnnotation<UI.Animated>()?.let { ann ->
                if(ann.timeToCompleteMs > 0) {
                    Thread.sleep(ann.timeToCompleteMs.toLong())
                }
            }
            if(sel.isHidden) {
                if(sel.hasAnnotation(UI.Visibility.Backend::class) || sel.hasAnyParentAnnotation(UI.Visibility.Backend::class)) {
                    throw SkipException("Селектор не может быть проверен, так как")
                }
                println("${sel.name} is hidden")
            }
            //sel должен бытьВидимым
            assertThat(sel.isVisible).isTrue()
            if(!BaseUiTest.config.disableAllScreenshots) {
                SeleniumBaseExecutor.enableScreenshots = true
                sel.screenshot()
                SeleniumBaseExecutor.enableScreenshots = false
            }
            sel.findAnnotation<UI.Animated>()?.let { ann ->
                if(ann.autoCloseMs > 0) {
                    sel.waitForDisappear(
                        Duration.ofMillis(ann.autoCloseMs.toLong())
                    )

                    assertThat(sel.isVisible).isFalse()
                    //sel должен бытьСкрытым
                }
            }
            if(sel.findAnnotation<UI.Visibility.Dynamic>()?.overlapped == true) {
                (sel.rootParent as Block).findWithAnnotation(UI.Widgets.ClickToClose::class)?.click()
            }
        }
    }
}