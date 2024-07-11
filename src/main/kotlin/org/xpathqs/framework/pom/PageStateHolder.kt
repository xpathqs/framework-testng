package org.xpathqs.framework.pom

import org.xpathqs.driver.log.action
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.model.clone
import org.xpathqs.driver.model.modelFromUi
import org.xpathqs.log.Log

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
            Log.action("PageStateHolder.save") {
                origin = if(base.isFilled) base.modelFromUi else base.clone()
            }
        }
    }

    override fun revert() {
        Log.action("PageStateHolder.revert") {
            origin!!.clone().fill(noSubmit = true, other = base.modelFromUi)
        }
    }

    override val model: IBaseModel
        get() = origin!!.clone()

}