package org.xpathqs.framework.pom

import io.qameta.allure.Allure
import org.testng.SkipException
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.block.findWithAnnotation
import org.xpathqs.core.selector.extensions.rootParent
import org.xpathqs.framework.validation.*
import org.xpathqs.framework.widgets.ValidationInput
import org.xpathqs.driver.extensions.click
import org.xpathqs.driver.extensions.screenshot
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.model.modelFromUi
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.framework.base.BaseUiTest
import org.xpathqs.framework.extensions.haveValidationErrorWithText
import org.xpathqs.framework.extensions.noValidationError
import org.xpathqs.framework.extensions.noValidationErrorWithText
import org.xpathqs.framework.extensions.should
import org.xpathqs.gwt.WHEN
import org.xpathqs.web.selenium.executor.SeleniumBaseExecutor
import kotlin.reflect.KMutableProperty

interface IValidationCheck {
    val model: IBaseModel?
    fun checkValidation(tc: ValidationTc)
}

abstract class ValidationCheck(
    var stateHolder: IPageStateHolder? = null
) : IValidationCheck {

    lateinit var uiModel: IBaseModel
    var methodCalled = false
    var applyModels = HashSet<IBaseModel>()

    override fun checkValidation(tc: ValidationTc) {
        Allure.getLifecycle().updateTestCase {
            it.name = "Validation of field '${tc.v.prop.name}' with type '${tc.rule}'"
        }

        stateHolder!!.save()
        val model = tc.model ?: model
        if(model?.isApplyModel == true && applyModels.contains(model)) {
            model.fill(true)
            applyModels.add(model)
        }

        if(model != null) {
            if(!(tc.rule.isConditionPassed(model))) {
                throw SkipException("Skipped due to the condition restriction")
            }
            if(!tc.skipRevert) {
                stateHolder!!.revert()
            }
            uiModel = model.modelFromUi
            when(tc.rule) {
                is Required<*> -> testRequired(tc, model)
                is Max<*> -> testMax(tc, model)
                is Date<*> -> testRule(tc.v, tc.rule, model)
                is MoreThan<*> -> testRule(tc.v, tc.rule, model)
                is MoreOrEqThan<*> -> testRule(tc.v, tc.rule, model)
                is DependsOn<*> -> testRule(tc.v, tc.rule, model)
                is DifferanceRange<*> -> testRule(tc.v, tc.rule, model)
                is Length<*> -> testRule(tc.v, tc.rule, model)
            }
            methodCalled = true
        }
    }

    private fun testMax(vc: ValidationTc, model: IBaseModel) {
        val block = model.findSelByProp(vc.v.prop)
        vc.rule as Max

        WHEN("значения меньшего на единицу от максимально допустимого") {
            vc.rule.input(vc.v.prop, model, (vc.rule.value - 1).toString())
            (block.rootParent as Page).removeInputFocus()
        }.THEN("ошибка валидации не должна отображаться") {
            block should noValidationError
            screenshot(block)
        }

        WHEN("значение равно допустимого") {
            vc.rule.input(vc.v.prop, model, (vc.rule.value).toString())
            (block.rootParent as Page).removeInputFocus()
        }.THEN("ошибка валидации не должна отображаться") {
            block should noValidationError
            screenshot(block)
        }

        WHEN("значение больше допустимого") {
            vc.rule.invalidate(vc.v.prop, uiModel)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("ошибка валидации должна отображаться c текстом '${vc.rule.hint}'") {
            block should haveValidationErrorWithText(vc.rule.hint)
            screenshot((block as ValidationInput).lblError)
        }

        WHEN("корректное значение повторно введено") {
            vc.rule.revert(vc.v.prop, stateHolder!!.model)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("ошибка валидации должна исчезнуть") {
            block should noValidationError
            screenshot(block)
        }
    }

    private fun testRequired(vc: ValidationTc, model: IBaseModel) {
        val block = model.findSelByProp(vc.v.prop)
        val rule = vc.rule as Required<*>

        WHEN("значения из обязательного удалено") {
            vc.rule.invalidate(vc.v.prop, uiModel)

            if(vc.v.parent.config.invalidationAction == InvalidationAction.FOCUS_LOST) {
                (block.rootParent as Page).removeInputFocus()
            } else {
                (block.rootParent as Page).findWithAnnotation(UI.Widgets.Submit::class)?.let {it.click()}
            }
        }.THEN("должна быть отображена ошибка валидации c текстом '${vc.rule.hint}'") {
            block should haveValidationErrorWithText(vc.rule.hint)
            screenshot((block as ValidationInput).lblError)
        }

        WHEN("корректное значение введено") {
            if(!vc.v.parent.isInvalidAtStart) {
                vc.rule.revert(vc.v.prop, stateHolder!!.model)
            } else {
                model.fill(vc.v.prop as KMutableProperty<*>)
            }
            if(vc.v.parent.config.invalidationAction == InvalidationAction.FOCUS_LOST) {
                (block.rootParent as Page).removeInputFocus()
            } else {
                (block.rootParent as Page).findWithAnnotation(UI.Widgets.Submit::class)?.let {it.click()}
            }
        }.THEN("ошибка валидации должна исчезнуть") {
            block should noValidationErrorWithText(rule.hint)
            screenshot(block)
        }
    }

    private fun testRule(v: Validation<*>, rule: Date<*>, model: IBaseModel) {
        val block = model.findSelByProp(v.prop)

        if(rule.past) {
            WHEN("введена дата в прошлом") {
                rule.input(v.prop, model, DateHelper.dayInPast)
                (block.rootParent as Page).removeInputFocus()
            }.THEN("Ошибка валидации не должна отображаться") {
                block should noValidationError
                screenshot(block)
            }
        } else {
            WHEN("введена дата в прошлом") {
                rule.input(v.prop, model, DateHelper.dayInPast)
                (block.rootParent as Page).removeInputFocus()
            }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
                block should haveValidationErrorWithText(rule.hint)
                screenshot((block as ValidationInput).lblError)
            }
        }

        if(rule.current) {
            WHEN("введена текущая дата") {
                rule.input(v.prop, model, DateHelper.currentDay)
                (block.rootParent as Page).removeInputFocus()
            }.THEN("Ошибка валидации не должна отображаться") {
                block should noValidationError
                screenshot(block)
            }
        } else {
            WHEN("введена текущая дата") {
                rule.input(v.prop, model, DateHelper.currentDay)
                (block.rootParent as Page).removeInputFocus()
            }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
                block should haveValidationErrorWithText(rule.hint)
                screenshot((block as ValidationInput).lblError)
            }
        }

        if(rule.future) {
            WHEN("введена дата в будущем") {
                rule.input(v.prop, model, DateHelper.dayInFuture)
                (block.rootParent as Page).removeInputFocus()
            }.THEN("Ошибка валидации не должна отображаться") {
                block should noValidationError
                screenshot(block)
            }
        } else {
            WHEN("введена дата в будущем") {
                rule.input(v.prop, model, DateHelper.dayInFuture)
                (block.rootParent as Page).removeInputFocus()
            }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
                block should haveValidationErrorWithText(rule.hint)
                screenshot((block as ValidationInput).lblError)
            }
        }
    }

    private fun testRule(v: Validation<*>, rule: MoreThan<*>, model: IBaseModel) {
        val block = model.findSelByProp(v.prop)

        WHEN("значение поля равно значению '${rule.dependsProp.name}'") {
            val originValue = rule.dependsProp.getter.call(model) as String
            rule.input(v.prop, model, originValue)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
            block should haveValidationErrorWithText(rule.hint)
            screenshot((block as ValidationInput).lblError)
        }

        WHEN("значение поля меньше значения '${rule.dependsProp.name}'") {
            rule.invalidate(v.prop, uiModel)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
            block should haveValidationErrorWithText(rule.hint)
            screenshot((block as ValidationInput).lblError)
        }
    }

    private fun testRule(v: Validation<*>, rule: MoreOrEqThan<*>, model: IBaseModel) {
        val block = model.findSelByProp(v.prop)

        WHEN("значение поля равно значению '${rule.dependsProp.name}'") {
            val originValue = rule.dependsProp.getter.call(model) as String
            rule.input(v.prop, model, originValue)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Ошибка валидации не должна отображаться") {
            block should noValidationError
            screenshot(block)
        }

        WHEN("значение поля меньше чем '${rule.dependsProp.name}'") {
            rule.invalidate(v.prop, uiModel)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
            block should haveValidationErrorWithText(rule.hint)
            screenshot((block as ValidationInput).lblError)
        }
    }

    private fun testRule(v: Validation<*>, rule: DifferanceRange<*>, model: IBaseModel) {
        val block = model.findSelByProp(v.prop)

        WHEN("значение поля за пределами значения '${rule.dependsProp.name}'") {
            rule.invalidate(v.prop, uiModel)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Должна отображаться ошибка валидации c текстом '${rule.hint}'") {
            block should haveValidationErrorWithText(rule.hint)
            screenshot((block as ValidationInput).lblError)
        }
    }

    private fun testRule(v: Validation<*>, rule: DependsOn<*>, model: IBaseModel) {
        val items = rule.lambda()
        items.forEach {
            val (k, r) = it
            Log.step("Проверка правила валидации когда значение полня '${rule.prop.name}' равно '$k'") {
                stateHolder!!.revert()
                model.setValueByProp(rule.prop as KMutableProperty<*>, k)
                checkValidation(
                    ValidationTc(
                        v = v,
                        rule = r,
                        skipRevert = true
                    )
                )
            }
        }
    }

    private fun testRule(v: Validation<*>, rule: Length<*>, model: IBaseModel) {
        val block = model.findSelByProp(v.prop)

        WHEN("длина значения соответствует заданным ограничениям") {
            model.fill(v.prop as KMutableProperty<*>)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Ошибка валидации не должна отображаться") {
            block should noValidationError
            screenshot(block)
        }

        WHEN("$rule для поля") {
            rule.invalidate(v.prop, model)
            (block.rootParent as Page).removeInputFocus()
        }.THEN("Ошибка валидации должна отображаться") {
            block should haveValidationErrorWithText(rule.hint)
            screenshot((block as ValidationInput).lblError)
        }
    }

    private fun screenshot(block: BaseSelector) {
        if(!BaseUiTest.config.disableAllScreenshots) {
            val before = SeleniumBaseExecutor.enableScreenshots
            SeleniumBaseExecutor.enableScreenshots = true
            block.screenshot()
            SeleniumBaseExecutor.enableScreenshots = before
        }
    }

    private fun isBlank(vc: ValidationTc) : Boolean {
        /*val sel = uiModel.findSelByProp(vc.v.prop) as? ValidationDropdownSelectFirst
        if(sel != null) {
            return sel.isEmpty
        }*/
        return uiModel.getValueByProp(vc.v.prop).isBlank()
    }
}