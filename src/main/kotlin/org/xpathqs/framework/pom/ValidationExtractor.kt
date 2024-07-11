package org.xpathqs.framework.pom

import org.xpathqs.framework.validation.IValidationModel
import org.xpathqs.framework.validation.Validation
import org.xpathqs.framework.validation.ValidationRule
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.model.default
import org.xpathqs.driver.navigation.base.IModelBlock
import org.xpathqs.framework.validation.ValidationConfig

//структура для теста по валидации
data class ValidationTc(
    val v: Validation<*>,
    val rule: ValidationRule<*>,
    val skipRevert: Boolean = false,
    val model: IBaseModel? = null,
    val vc: ValidationConfig
)

interface IValidationExtractor {
    val validations: Collection<ValidationTc>
}

class ValidationExtractor(
    private val page: IModelBlock<*>?
) : IValidationExtractor {

    val model: IValidationModel<*>?
        get() {
            return page?.invoke()?.let { m ->
                if(!m.isInitialized) {
                    m.default()
                }
                return m as? IValidationModel<*>
            }
        }

    override val validations: Collection<ValidationTc>
        get() {
            val res = ArrayList<ValidationTc>()

            model?.validations?.rules?.forEach { validation ->
                validation.rules.forEach {
                    res.add(
                        ValidationTc(
                            v = validation,
                            rule = it,
                            vc = model!!.validations.config
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
                                model = model as IBaseModel,
                                vc = model.validations.config
                            )
                        )
                    }
                }
            }

            return res
        }
}