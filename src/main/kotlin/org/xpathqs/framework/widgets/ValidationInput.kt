package org.xpathqs.framework.widgets

import org.xpathqs.core.annotations.Name
import org.xpathqs.core.reflection.freeze
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.block.Block
import org.xpathqs.core.selector.extensions.contains
import org.xpathqs.core.selector.extensions.parentCount
import org.xpathqs.core.selector.extensions.text
import org.xpathqs.core.selector.extensions.textNotEmpty
import org.xpathqs.core.util.SelectorFactory.tagSelector
import org.xpathqs.core.util.SelectorFactory.textSelector
import org.xpathqs.driver.constants.Global
import org.xpathqs.driver.extensions.*
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.log.Log
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.driver.widgets.IFormInput
import org.xpathqs.driver.widgets.IFormRead
import org.xpathqs.framework.extensions.бытьВидима
import org.xpathqs.framework.extensions.бытьСкрыта

import org.xpathqs.web.factory.HTML
import java.time.Duration

open class Input(
    base: BaseSelector,

    @UI.Visibility.Always
    @UI.Widgets.Input
    val input: BaseSelector = HTML.input(),

) : Block(base), IFormInput, IFormRead {
    constructor(inputName: String, baseLevel: Int, secret: Boolean = false) :
            this(
                base = (tagSelector() contains HTML.input(name = inputName)).parentCount(baseLevel),
                input = if(secret) HTML.secretInput(name = inputName) else HTML.input(name = inputName),
            )

    constructor(input: BaseSelector, baseLevel: Int) :
            this(
                base = input.freeze().parentCount(baseLevel),
                input = input,
            )

    override fun input(value: String, model: IBaseModel?) {
        if(readString(model) != value) {
            if(model != null) {
                input.input(value, model = model)
            } else {
                input.input(value)
            }
        }
    }

    override fun isDisabled(): Boolean {
        return input.isDisabled
    }

    override fun readString(model: IBaseModel?): String {
        try{
            if(input.tag.lowercase() == "textarea") {
                return input.getAttr(Global.TEXT_ARG, model)
            }
            return input.getAttr("value", model)
        } catch (e: Exception) {
            Log.info("Can't read 'value' from ${input.name}")
        }
        return ""
    }

    open val isEmpty: Boolean
        get() {
            return false
        }

    override fun focus() {
        input.click()
    }
}

open class ValidationInput(
    base: BaseSelector,

    input: BaseSelector = HTML.input(),

    @UI.Widgets.ValidationError
    @UI.Visibility.Dynamic
    @Name("Validation Error Label")
    val lblError: BaseSelector = HTML.span()
) : Input(base, input) {
    constructor(inputName: String, errorLabel: String, baseLevel: Int, secret: Boolean = false) :
        this(
            base = (tagSelector() contains HTML.input(name = inputName)).parentCount(baseLevel),
            input = if(secret) HTML.secretInput(name = inputName) else HTML.input(name = inputName),
            lblError = textSelector(errorLabel)
        )

    constructor(input: BaseSelector, errorLabel: String, baseLevel: Int) :
        this(
            base = input.freeze().parentCount(baseLevel),
            input = input,
            lblError = textSelector(errorLabel)
        )

    fun assertNoValidationError() {
        lblError.waitForDisappear(Duration.ofSeconds(1))
        lblError.бытьСкрыта()
    }

    fun assertNoValidationError(text: String) {
        lblError.text(text).waitForDisappear(Duration.ofSeconds(1))
        lblError.text(text).бытьСкрыта()
    }

    fun assertValidationError() {
        lblError.waitForVisible(Duration.ofSeconds(1))
        lblError.бытьВидима()
    }

    fun assertValidationErrorText(text: String) {
        lblError.text(text).waitForVisible(Duration.ofSeconds(1))
        lblError.text(text).бытьВидима()
    }

    override fun isValidationError(): Boolean {
        return lblError.textNotEmpty().isVisible
    }
}