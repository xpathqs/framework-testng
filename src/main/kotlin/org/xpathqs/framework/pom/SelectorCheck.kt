package org.xpathqs.framework.pom

import assertk.assertThat
import assertk.fail
import io.qameta.allure.Allure
import org.testng.SkipException
import org.xpathqs.core.selector.base.*
import org.xpathqs.core.selector.block.allInnerSelectors
import org.xpathqs.core.selector.block.findWithAnnotation
import org.xpathqs.core.selector.extensions.parents
import org.xpathqs.core.selector.extensions.rootParent
import org.xpathqs.driver.extensions.*
import org.xpathqs.log.Log
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.driver.navigation.base.IPageCallback
import org.xpathqs.driver.navigation.base.IPageSpecificState
import org.xpathqs.framework.base.BaseUiTest
import org.xpathqs.framework.extensions.бытьВидимым
import org.xpathqs.framework.extensions.бытьСкрытым
import org.xpathqs.framework.extensions.должен
import org.xpathqs.gwt.GIVEN
import org.xpathqs.log.style.StyleFactory
import org.xpathqs.web.selenium.executor.SeleniumBaseExecutor
import java.time.Duration

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
                if(sel.hasAnnotation(UI.Visibility.Backend::class)
                    || sel.hasAnyParentAnnotation(UI.Visibility.Backend::class)
                    || sel.hasAnyParentAnnotation(UI.Visibility.OneOf::class)
                    || sel.hasAnnotation(UI.Visibility.OneOf::class)
                    || sel.hasAnnotation(UI.Visibility.Backend::class)
                ) {
                    throw SkipException("Наличие селектора на странице устанавливается на стороне сервера")
                }
                val ann = sel.findAnyParentAnnotation<UI.Visibility.Dynamic>()
                if (ann != null) {
                    assertThat(true)
                    if(ann.canApplyForGlobalState != UI.Visibility.UNDEF_STATE && ann.canApplyForGlobalStateGroup != UI.Visibility.UNDEF_STATE) {
                        val actualState = (sel.rootParent as? IPageSpecificState)?.pageState(ann.canApplyForGlobalStateGroup)
                        if(actualState != ann.canApplyForGlobalState) {
                            throw SkipException("Текущее состояние страницы не позволяет отобразить селектор")
                        }
                    }
                }

                e.printStackTrace()
                fail(e.message ?: "")
            }
        }
        Log.action("Должен быть видимым", GIVEN.THEN) {
            sel.findAnnotation<UI.Animated>()?.let { ann ->
                if(ann.timeToCompleteMs > 0) {
                    wait(ann.timeToCompleteMs.ms, "wait according to animation annotation")
                }
            }
            if(sel.isHidden) {
                if(sel.hasAnnotation(UI.Visibility.Backend::class)
                    || sel.hasAnyParentAnnotation(UI.Visibility.Backend::class)
                    || sel.hasAnyParentAnnotation(UI.Visibility.OneOf::class)
                ) {
                    throw SkipException("Селектор не может быть проверен, так как")
                }
                println("${sel.name} is hidden")
            }
            sel должен бытьВидимым
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
                    sel должен бытьСкрытым
                }
            }
            if(sel.findAnnotation<UI.Visibility.Dynamic>()?.overlapped == true) {
                (sel.rootParent as? org.xpathqs.core.selector.block.Block)?.findWithAnnotation(UI.Widgets.ClickToFocusLost::class)?.click()
            } else {
                (sel.base as? org.xpathqs.core.selector.block.Block)?.allInnerSelectors?.filter {
                    it.findAnnotation<UI.Visibility.Dynamic>()?.overlapped == true
                }?.firstOrNull {
                    it.isVisible
                }?.let {
                    (sel.rootParent as? org.xpathqs.core.selector.block.Block)?.findWithAnnotation(UI.Widgets.ClickToFocusLost::class)?.click()
                    return@action
                }

                sel.parents.filter {
                    it.findAnnotation<UI.Visibility.Dynamic>()?.overlapped == true
                }.firstOrNull {
                    it.isVisible
                }?.let {
                    (sel.rootParent as? org.xpathqs.core.selector.block.Block)?.findWithAnnotation(UI.Widgets.ClickToFocusLost::class)?.click()
                }
            }
        }
    }
}