package org.xpathqs.framework.extensions

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.xpathqs.core.selector.args.ValueArg
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.base.findAnnotation
import org.xpathqs.core.selector.base.findAnyParentAnnotation
import org.xpathqs.core.selector.block.Block
import org.xpathqs.core.selector.block.allInnerSelectors
import org.xpathqs.core.selector.extensions.addArg
import org.xpathqs.core.selector.selector.Selector
import org.xpathqs.core.util.SelectorFactory.attrSelector
import org.xpathqs.core.util.SelectorFactory.tagSelector
import org.xpathqs.framework.pom.Page
import org.xpathqs.framework.widgets.ValidationInput
import org.xpathqs.driver.extensions.*
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.navigation.base.ILoadable
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.model.clone
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.driver.navigation.impl.PageState.Companion.isStaticSelector
import org.xpathqs.driver.widgets.IFormRead

import org.xpathqs.log.style.StyleFactory.selectorName
import org.xpathqs.log.style.StyleFactory.text
import java.time.Duration

fun BaseSelector.beVisible() {
    if(this is Page) {
        this.determination.exist.forEach {
            if(it.isHidden) {
                println("$this is hidden")
            }
            assertThat(it.isVisible, "${it.name} должен быть видимым")
                .isEqualTo(true)
        }
    } else {
        val isVisible = this.isVisible
        if(!isVisible) {
            Log.error("$this is hidden")
        }
        assertThat(isVisible, "$name должен быть видимым")
            .isEqualTo(true)
    }
}

fun BaseSelector.beHidden() {
    if(isVisible) {
        Log.error("$this is visible")
    }
    assertThat(this.isHidden)
        .isEqualTo(true)
}

fun BaseSelector.haveText(text: String) {
    Log.step(text("Селектор ") + selectorName(this.name) + text(" должен иметь текст: '$text'")) {
        val actual = if(this is IFormRead) {
            this.readString()
        } else {
            this.text
        }

        if(actual != text) {
            Log.error("not eq")
        }

        assertThat(actual)
            .isEqualTo(text)
    }
}

fun BaseSelector.containsText(text: String) {
    Log.step(text("Селектор ") + selectorName(this.name) + text(" должен содержать текст: '$text'")) {
        assertThat(this.text.contains(text))
            .isEqualTo(true)
    }
}

fun BaseSelector.containsText(expectedItems: Collection<String>) {
    //val items = if(this is ValidationDropdownSelectFirst) getItems() else this.textItems
    assertThat(this.textItems)
        .isEqualTo(expectedItems)
}

open class ExpectedExec(
    private val selector: BaseSelector,
    private val lambda: BaseSelector.() -> Unit
) {
    fun check() {
        selector.lambda()
    }
}

open class Expected(
    val lambda: BaseSelector.() -> Unit)

open class ExpectedText(
    lambda: BaseSelector.() -> Unit
): Expected(lambda)

open class ExpectedInt(
    lambda: BaseSelector.() -> Unit
): Expected(lambda)

open class ExpectedVisible(
    lambda: BaseSelector.() -> Unit): Expected(lambda)

open class ExpectedValidation(val msg: String="",
    lambda: ValidationInput.() -> Unit) : ExpectedVisible(lambda as BaseSelector.() -> Unit)

val beVisible = ExpectedVisible {
    this.beVisible()
}

val beHidden = ExpectedVisible {
    this.beHidden()
}

fun haveText(text: String) = ExpectedText {
    this.haveText(text)
}

fun containsNumber(num: Int) = ExpectedInt {
    Log.step(text("Селектор ") + selectorName(this.name) + text(" должен содержать число: '$num'")) {
        assertThat(this.text.filter { it.isDigit() }.toInt())
            .isEqualTo(num)
    }
}

fun haveCount(num: Int) = ExpectedInt {
    Log.step(text("Селектор ") + selectorName(this.name) + text(" должен иметь '$num' число элементов: ")) {
        assertThat(this.count)
            .isEqualTo(num)
    }
}

fun containsText(text: String) = ExpectedText {
    this.containsText(text)
}

fun containsText(items: Collection<String>) = ExpectedText {
    this.containsText(items)
}

val noValidationError = ExpectedValidation("Ошибка валидации должна отсутствовать для ") {
    this.assertNoValidationError()
}

fun noValidationErrorWithText(text: String) = ExpectedValidation("Ошибка валидации должна отсутствовать для ") {
    this.assertNoValidationError(text)
}

fun haveValidationErrorWithText(text: String) = ExpectedValidation("Ошибка валидации должна быть отображена для ") {
    this.assertValidationErrorText(text)
}

infix fun BaseSelector.should(arg: Expected) {
    if(arg is ExpectedValidation) {
        Log.step(text(arg.msg) + selectorName(this.name)) {
            this.waitForVisible()
            Log.xpath(this)
        }
    } else if(arg is ExpectedVisible) {
        Log.step(text("Селектор ") + selectorName(this.name) + text(" должен быть видимым")) {
            if(this is ILoadable) {
                this.waitForLoad(Duration.ofSeconds(5))
            } else {
                this.waitForVisible()
                Log.xpath(this)
            }
        }
    }
    ExpectedExec(this, arg.lambda).check()
}

fun BaseSelector.clickUntilVisible() {
    while(this.isVisible) {
        try {
            this.click()
        } catch (e: Exception) {
            invalidateCache()
            return
        }
    }
}

fun BaseSelector.waitForValueChanged(duration: Duration = Duration.ofSeconds(5)) {
    val originValue = this.text
    val startTs = System.currentTimeMillis()
    var cond1 = originValue == this.text
    var cond2 = (System.currentTimeMillis() - startTs) < duration.toMillis()

    while (cond1 && cond2) {
        Thread.sleep(500)
        invalidateCache()

        cond1 = originValue == this.text
        cond2 = (System.currentTimeMillis() - startTs) < duration.toMillis()
    }

    println()
}

fun<T: IBaseModel> T.setup(lambda: T.() -> Unit) = clone().applyModel { lambda() }

fun invalidateCache() {
    tagSelector("asd").waitForVisible(Duration.ofMillis(10))
}

inline fun <T: IBaseModel, R> applyModel(receiver: T, crossinline block: T.() -> R): T {
    Log.step("Заполнить форму") {
        IBaseModel.disableUiUpdate()
        receiver.block()
        IBaseModel.enableUiUpdate()
        receiver.submit()
    }

   // receiver.containers.first().screenshot(false)
    return receiver
}

fun testId(value: String): Selector {
    return attrSelector(
        name = "data-testid",
        value = value
    )
}

fun <T:IBaseModel> T.submit(lambda: T.()->Unit) {
    this.lambda()
    this.submit()
}

fun <T:IBaseModel> T.fill(lambda: T.()->Unit) {
    this.lambda()
    this.fill(noSubmit = true)
}

fun <T: BaseSelector> T.notHidden() : T {
    this.addArg(ValueArg("not(ancestor-or-self::*[@hidden])"))
    return this
}


fun Block.getStaticSelectorsWithState(state: Int, includeContains: Boolean = true): Collection<BaseSelector> {
    val res = ArrayList<BaseSelector>()
    if(includeContains) {
        this.annotations.filterIsInstance<UI.Nav.PathTo>().forEach {
            if(it.selfPageState == state) {
               // it.contains.forEach {
                if(it.contain != Block::class) {
                    res.addAll(
                        it.contain.objectInstance?.allInnerSelectors ?: emptyList()
                    )
                }

              //  }
            }
        }
    }

    return (res + this.allInnerSelectors)
        .filter {
            it.findAnnotation<UI.Visibility.State>()?.value == state
                || it.findAnyParentAnnotation<UI.Visibility.State>()?.value == state
        }.filter {
            isStaticSelector(it)
        }
}