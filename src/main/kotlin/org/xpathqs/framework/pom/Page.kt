package org.xpathqs.framework.pom

import org.xpathqs.core.selector.NullSelector
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.base.ISelector
import org.xpathqs.core.selector.base.hasAnnotation
import org.xpathqs.core.selector.base.hasAnyParentAnnotation
import org.xpathqs.core.selector.block.allInnerSelectors
import org.xpathqs.core.selector.block.findWithAnnotation
import org.xpathqs.driver.extensions.click
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.navigation.Navigator
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.driver.navigation.base.*
import org.xpathqs.driver.navigation.impl.Loadable
import org.xpathqs.driver.navigation.impl.NavigableDetermination
import org.xpathqs.framework.base.BaseUiTest
import org.xpathqs.web.WebPage
import java.time.Duration

open class Page(
    base: ISelector= NullSelector()
): WebPage(
    base = base
), INavigableDeterminationDelegate, ILoadableDelegate, IPageNavigator, IBlockSelectorNavigation {

    override val navigator: Navigator
        get() {
            return DefaultNavigator.navigator.get()
        }

    fun removeInputFocus() {
        this.findWithAnnotation(UI.Widgets.ClickToClose::class)?.let {
            it.click()
            Thread.sleep(500) //небольшое ожидание, для того чтоб JS успел что-то отрендерить
        }
    }

    open fun open(param: String) {}

    override val nav: NavigableDetermination
        get() = NavigableDetermination(navigator, this)

    override fun navigate(elem: ISelector, navigator: INavigator) {
        nav.navigate(elem, navigator)
    }

    override val loadable by lazy {
        Loadable(this)
    }

    override fun afterReflectionParse() {
        navigator.register(this)
        if(this.determination.exist.isEmpty()) {
            Log.error("У страницы $this нет элементов для определения")
        }
    }

    open fun refresh() {
        if(url.isNotEmpty()) {
            BaseUiTest.commonData.get().driver.get(url)
        } else {
            BaseUiTest.commonData.get().driver.navigate().refresh()
        }
    }

    open fun openByUrl() {
        refresh()
        waitForLoad(Duration.ofSeconds(30))
    }
}

val Page.allValidationSelectors: Collection<BaseSelector>
    get() = allInnerSelectors.filter {
        it.hasAnnotation(UI.Widgets.ValidationError::class)
    }

val Page.allPresentSelectors: Collection<BaseSelector>
    get() = allInnerSelectors.filter {
        !it.hasAnnotation(UI.Visibility.Dynamic::class)
                && !it.hasAnyParentAnnotation(UI.Visibility.Dynamic::class)
    }