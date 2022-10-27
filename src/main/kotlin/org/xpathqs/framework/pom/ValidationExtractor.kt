package org.xpathqs.framework.pom

import org.xpathqs.framework.validation.IValidationModel
import org.xpathqs.framework.validation.Validation
import org.xpathqs.framework.validation.ValidationRule
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.navigation.base.IModelBlock

//структура для теста по валидации
data class ValidationTc(
    val v: Validation<*>,
    val rule: ValidationRule<*>,
    val skipRevert: Boolean = false,
    val model: IBaseModel? = null
)

interface IValidationExtractor {
    val validations: Collection<ValidationTc>
}

class ValidationExtractor(
    private val page: IModelBlock<*>?
) : IValidationExtractor {

    val model: IValidationModel<*>?
        get() {
            return page?.invoke() as? IValidationModel<*>
        }

    override val validations: Collection<ValidationTc>
        get() {
            val res = ArrayList<ValidationTc>()

            model?.validations?.rules?.forEach { validation ->
                validation.rules.forEach {
                    res.add(
                        ValidationTc(
                            v = validation,
                            rule = it
                        )
                    )
                }
            }
            return res
        }
}

class ModelListValidationExtractor(
    val models: Collection<IValidationModel<*>>
) : IValidationExtractor {

    override val validations: Collection<ValidationTc>
        get() {
            val res = ArrayList<ValidationTc>()
            models.forEach { model ->
                model.validations.rules.forEach { validation ->
                    validation.rules.forEach {
                        res.add(
                            ValidationTc(
                                v = validation,
                                rule = it,
                                model = model as IBaseModel
                            )
                        )
                    }
                }
            }

            return res
        }
}