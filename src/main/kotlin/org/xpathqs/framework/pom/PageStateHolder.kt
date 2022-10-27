package org.xpathqs.framework.pom

import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.model.clone
import org.xpathqs.driver.model.modelFromUi

interface IPageStateHolder {
    fun save()
    fun revert()
    val model: IBaseModel
}

class PageStateHolder(
    private val base: IBaseModel
) : IPageStateHolder {
    private var origin: IBaseModel? = null

    override fun save() {
        if(origin == null) {
            origin = if(base.isFilled) base.modelFromUi else base.clone()
        }
    }

    override fun revert() {
        origin!!.clone().fill(noSubmit = true, base.modelFromUi)
    }

    override val model: IBaseModel
        get() = origin!!.clone()

}