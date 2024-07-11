package org.xpathqs.framework.extensions

import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.gwt.GIVEN
import org.xpathqs.gwt.When
import org.xpathqs.log.style.StyledString

fun BaseSelector.бытьВидима() {
    this.beVisible()
}

fun BaseSelector.бытьСкрыта() {
    this.beHidden()
}

fun BaseSelector.иметьТекст(text: String) {
    this.haveText(text)
}

fun BaseSelector.неСодержатьТекст(text: String) {
    this.notContainsText(text)
}

fun BaseSelector.содержатьТекст(text: String) {
    this.containsText(text)
}

fun BaseSelector.содержатьТекст(expectedItems: Collection<String>) {
    this.containsText(expectedItems)
}

inline fun <T: IBaseModel, R> применитьМодель(receiver: T, crossinline block: T.() -> R): T {
    return applyModel(receiver, block)
}

val бытьВидима = ExpectedVisible {
    this.бытьВидима()
}
val бытьВидимым = бытьВидима
val бытьВидимыми = бытьВидима

val бытьСкрыта = ExpectedVisible {
    this.бытьСкрыта()
}
val бытьСкрытым = бытьСкрыта
val бытьСкрытыми = бытьСкрыта

fun иметьТекст(text: String) = ExpectedText {
    this.иметьТекст(text)
}

fun содержатьЧисло(num: Int) = containsNumber(num)

fun иметьКоличество(num: Int) = haveCount(num)

fun содержатьТекст(text: String) = containsText(text)

fun содержатьТекст(items: Collection<String>) = containsText(items)

val отсутствоватьОшибкаВалидации = noValidationError

fun отсутствоватьОшибкаВалидацииТекстом(text: String) = noValidationErrorWithText(text)

fun отображатьсяОшибкаВалидацииТекстом(text: String) = haveValidationErrorWithText(text)

infix fun BaseSelector.должна(arg: Expected) = should(arg)

infix fun BaseSelector.должно(arg: Expected) = должна(arg)
infix fun BaseSelector.должны(arg: Expected) = должна(arg)
infix fun BaseSelector.должен(arg: Expected) = должна(arg)


/*
fun<G:Any, W:Any> When<G,W>.ТОГДА(expected: W) = ТОГДА("", expected)
fun<G:Any, W:Any> When<G,W>.ТОГДА(msg: String, expected: W) = ТОГДА(StyledString(msg), expected)
fun<G:Any, W:Any> When<G,W>.ТОГДА(msg: StyledString, expected: W): When<G, W> {
    GIVEN.log.action(msg, GIVEN.THEN) {
        GIVEN.gwtAssert.equals(actual, expected)
    }
    return this
}

*/

fun<G:Any, W> When<G,W>.ТОГДА(f: When<G, W>.()->Unit) = ТОГДА("", f)
fun<G:Any, W> When<G,W>.ТОГДА(msg: String, f: When<G, W>.()->Unit) = ТОГДА(StyledString(msg), f)
fun<G:Any, W> When<G,W>.ТОГДА(msg: StyledString, f: When<G, W>.()->Unit): When<G, W> {
    GIVEN.logEvaluator.THEN(msg, this, f)
    return this
}
