package org.xpathqs.framework.validation

import org.xpathqs.framework.widgets.ValidationInput
import org.xpathqs.driver.extensions.click
import org.xpathqs.driver.navigation.annotations.Model
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.widgets.IFormInput
import org.xpathqs.driver.widgets.IFormRead
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

abstract class ValidationRule<T>(var hint: String = "") {
    var conditionLambda: ((obj: T)-> Boolean)? = null

    infix fun hint(v: String): ValidationRule<T> {
        this.hint = v
        return this
    }

    infix fun condition(lambda: (obj: T)-> Boolean): ValidationRule<T> {
        this.conditionLambda = lambda
        return this
    }

    open fun input(prop: KProperty1<*, *>, model: IBaseModel, str: String) {
        if(prop is KMutableProperty<*>) {
            model.setValueByProp(prop, str)
        }
    }

    abstract fun invalidate(prop: KProperty1<*, *>, model: IBaseModel)

    open fun revert(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            obj.fill(prop)
        }
    }

    override fun toString(): String {
        return "Validation"
    }

    fun isDate(prop: KProperty1<*, *>) : Boolean {
        return prop.annotations.find {
            it.annotationClass === Model.DataTypes.Date::class
        } != null
    }

    fun isConditionPassed(model: IBaseModel):Boolean {
        if(conditionLambda != null) {
            return conditionLambda!!(model as T)
        }
        return true
    }
}

open class Required<T>(
    val checkErrorWithText: Boolean = false
): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            val sel = obj.findSelByProp(prop)
            if(sel is IFormInput) {

                obj.makeVisible(sel, prop, disabled = sel.isDisabled())

                if(sel is IFormRead) {
                    if(sel.readString().isEmpty()) {
                        if(prop.findAnnotation<Model.DataTypes.Number>() != null) {
                            sel.input("1")
                        }
                    }
                }

                sel.clear()

                if(sel is ValidationInput) {
                    sel.focus()
                } else {
                    sel.click()
                }
            }
        }
    }

    override fun toString(): String {
        return "Обязательное поле"
    }
}


open class DependsOn<T>(
    val prop: KProperty1<*, *>,
    val lambda: ()->Collection<Pair<*, ValidationRule<*>>>
): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            val sel = obj.findSelByProp(prop)
            if(sel is IFormInput) {
                if(sel.isDisabled()) {
                    obj.makeVisible(sel, prop, disabled = true)
                }

                sel.clear()
            }
        }
    }

    override fun toString(): String {
        return "Различные значения поля \"${prop.name}\""
    }
}

open class Date<T>(
    val past: Boolean = true,
    val current: Boolean = true,
    val future: Boolean = true
) : ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, model: IBaseModel) {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        var res = "Дата должна быть"

        if(past) {
            res += " в прошлом"
        }
        if(current) {
            if(past) {
                res += ","
            }
            res += " в настоящем"
        }
        if(future) {
            if(past || current) {
                res += ","
            }
            res += " в будущем"
        }

        return res
    }
}

open class Items<T>: ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, model: IBaseModel) {

    }
}

open class ShouldMatch<T>: ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {

    }
}

open class Length<T>(
    val moreThen: Int? = null,
    val lessThen: Int? = null,
    val  eq: Int? = null
): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            val sel = obj.findSelByProp(prop)
            if(sel is IFormInput) {
                if(sel.isDisabled()) {
                    obj.makeVisible(sel, prop, disabled = true)
                }

                if(sel is IFormRead) {
                    //if(prop.findAnnotation<Model.DataTypes.Number>() != null) {
                        val value = if(moreThen != null) {
                            "1".repeat(moreThen)
                        } else if(lessThen != null) {
                            "1".repeat(lessThen + 1)
                        } else if(eq != null) {
                            "1".repeat(eq - 1)
                        } else throw Exception("")
                        sel.input(value)
                    //}
                }

                if(sel is ValidationInput) {
                    sel.focus()
                } else {
                    sel.click()
                }
            }
        }
    }

    override fun toString(): String {
        if(moreThen != null) {
            return "Длинна > $moreThen"
        } else if(lessThen != null) {
            return "Длинна < $lessThen"
        } else if(eq != null) {
            return "Длинна = $eq"
        }
        return ""
    }
}

open class Max<T>(val value: Int): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            input(prop, obj, (value + 1).toString())
        }
    }

    override fun toString(): String {
        return "Допустимое значение >= $value"
    }
}

open class MoreThan<T>(val dependsProp: KProperty1<*, *>): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            if(isDate(prop)) {
                val baseDate = dependsProp.getter.call(obj) as String
                input(prop, obj, DateHelper.minusDay(baseDate))
            } else {

            }
        }
    }

    override fun toString(): String {
        return "Допустимое значение > ${dependsProp.name}"
    }
}

open class MoreOrEqThan<T>(val dependsProp: KProperty1<*, *>): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            if(isDate(prop)) {
                val baseDate = dependsProp.getter.call(obj) as String
                input(prop, obj, DateHelper.minusDay(baseDate))
            } else {

            }
        }
    }

    override fun toString(): String {
        return "Допустимое значение > ${dependsProp.name}"
    }
}

open class DifferanceRange<T>(val dependsProp: KProperty1<*, *>, val interval: Any): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {
        if(prop is KMutableProperty<*>) {
            if(isDate(prop)) {
                val baseDate = dependsProp.getter.call(obj) as String

                if(interval is Period) {
                    input(prop, obj, DateHelper.plus(baseDate, interval.months.toLong(), interval.days.toLong() + 1))
                }
            } else {

            }
        }
    }

    override fun toString(): String {
        return "Разница в диапазоне относительно поля '${dependsProp.name}' в пределах '$interval'"
    }
}

open class DifferanceRangeFromNow<T>(
    val interval: Any,
    val inPast: Boolean = true //в прошлом или в будущем
): ValidationRule<T>() {
    override fun invalidate(prop: KProperty1<*, *>, obj: IBaseModel) {

    }

    override fun toString(): String {
        return "Разница в диапазоне относительно текущей даты в пределах '$interval'"
    }
}

enum class PropType {UNDEF, STRING, INT, DATE, EMAIL}

class PropertyValidation(
    var datatype: PropType = PropType.UNDEF,
    val property: KProperty1<*,*>? = null,
    var required: Boolean = true
)


class Validations<T: Any>(val config: ValidationConfig = ValidationConfig()) {

    lateinit var prop: KProperty1<*, *>

    val rules = ArrayList<Validation<T>>()
    lateinit var defaultModel: T

    var isInvalidAtStart = false
    var isCleanAtStart = false
    var validationStartsAfterFirstInput = false
    var applyModel = false

    operator fun KProperty1<*,*>.invoke(l: Validation<T>.() -> Unit) {
        this@Validations.prop = this
        val res = Validation(this, this@Validations)
        res.l()
        rules.add(res)
    }

    fun state(stateLambda: (T) -> Unit, l: Validations<T>.() -> Unit) {
        //stateLambda(defaultModel)
        this.l()
    }

    fun invalidAtStartUp() {
        isInvalidAtStart = true
    }

    fun cleanAtStart() {
        isCleanAtStart = true
    }

    fun validationAfterFirstInput() {
        validationStartsAfterFirstInput = true
    }

    fun applyModel() {
        applyModel = true
    }
}

class Validation<T: Any>(
    val prop: KProperty1<*, *>,
    val parent: Validations<T>
) {

    val rules = ArrayList<ValidationRule<T>>()

    fun dependsOn(prop: KProperty1<*, *>, lambda: ()->Collection<Pair<*, ValidationRule<*>>>) : ValidationRule<T> {
        val r = DependsOn<T>(prop, lambda)
        rules.add(r)
        return r
    }

    fun required(checkErrorWithText: Boolean = false): ValidationRule<T> {
        val r = Required<T>(checkErrorWithText)
        rules.add(r)
        return r
    }

    fun date(past: Boolean = true, current: Boolean = true, future: Boolean = true): ValidationRule<T> {
        val r = Date<T>(past, current, future)
        rules.add(r)
        return r
    }

    fun items(vararg vals: String): ValidationRule<T> {
        val r = Items<T>()
        rules.add(r)
        return r
    }

    fun shouldMatch(): ValidationRule<T> {
        val r = ShouldMatch<T>()
        rules.add(r)
        return r
    }

    fun length(
        moreThen: Int? = null,
        lessThen: Int? = null,
        eq: Int? = null
    ): ValidationRule<T> {
        val r = Length<T>(
            moreThen = moreThen,
            lessThen = lessThen,
            eq = eq
        )
        rules.add(r)
        return r
    }

    fun moreThen(p: KProperty1<*,*>): ValidationRule<T> {
        val r = MoreThan<T>(p)
        rules.add(r)
        return r
    }

    fun moreOrEqThen(p: KProperty1<*,*>): ValidationRule<T> {
        val r = MoreOrEqThan<T>(p)
        rules.add(r)
        return r
    }

    fun differencesRange(source: KProperty1<*,*>, interval: Period): ValidationRule<T> {
        val r = DifferanceRange<T>(source, interval)
        rules.add(r)
        return r
    }

    fun differencesRange(interval: Period): ValidationRule<T> {
        val r = DifferanceRangeFromNow<T>(interval)
        rules.add(r)
        return r
    }

    fun max(value: Int): ValidationRule<T> {
        val r = Max<T>(value)
        rules.add(r)
        return r
    }

    infix fun Validation<T>.lessThan(prop: KProperty1<*,*>): Validation<T> {
        return this
    }

}

fun<T: Any> validationFor(config: ValidationConfig = ValidationConfig(), lambda: Validations<T>.()->Unit) : Validations<T> {
    val f = Validations<T>(config)
    f.lambda()
    return f
}

object DateHelper {
    val currentDay: String
        get() = toString(ldt)

    val dayInPast: String
        get() = toString(ldt.minusDays(1))

    val dayInFuture: String
        get() = toString(ldt.plusDays(1))

    private val ldt = LocalDateTime.now()
    private val pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun toString(l: LocalDateTime = ldt): String {
        return "${l.dayOfMonth}.${l.monthValue}.${l.year}"
    }

    fun toString(l: LocalDate): String {
        return "${l.dayOfMonth}.${l.monthValue}.${l.year}"
    }

    fun plusDay(date: String): String {
        val parsedDay = LocalDate.parse(date, pattern)
        return toString(parsedDay.plusDays(1))
    }

    fun minusDay(date: String): String {
        val parsedDay = LocalDate.parse(date, pattern)
        return toString(parsedDay.minusDays(1))
    }

    fun plus(date: String, months: Long, days: Long): String {
        val parsedDay = LocalDate.parse(date, pattern)
        return toString(parsedDay.plusMonths(months).plusDays(days))
    }

    fun minus(date: String, months: Long, days: Long): String {
        val parsedDay = LocalDate.parse(date, pattern)
        return toString(parsedDay.minusMonths(months).minusDays(days))
    }

    fun processDate(str: String): String {
        if(str.isEmpty() || str.equals("null", ignoreCase = true)) {
            return ""
        }

        val split = str.split(".")
        val d = if(split[0].length == 1) "0"+split[0] else split[0]
        val m = if(split[1].length == 1) "0"+split[1] else split[1]
        val y = split[2]

        return "$d.$m.$y"
    }
}

interface IValidationModel<T: Any> {
    val validations: Validations<T>
}

val IBaseModel.isApplyModel: Boolean
    get() {
        return (this as? IValidationModel<*>)?.validations?.applyModel == true
    }

enum class InvalidationAction {FOCUS_LOST, SUBMIT}

class ValidationConfig(
    val invalidationAction: InvalidationAction = InvalidationAction.FOCUS_LOST,
    val reloadByUrlBeforeCheck: Boolean = false
)