package org.xpathqs.framework.pom

import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.base.findAnnotation
import org.xpathqs.core.selector.base.findAnyParentAnnotation
import org.xpathqs.core.selector.block.allInnerSelectors
import org.xpathqs.driver.navigation.annotations.UI
import org.xpathqs.driver.navigation.annotations.UI.Visibility.Companion.UNDEF_STATE

data class NavigationTc(
    val from: BaseSelector,
    val to: BaseSelector,
    val state: Int = UNDEF_STATE,

    val clickSelector: BaseSelector? = null
) {
    fun getName() : String {
        return "Navigation from '${this.from.name}' to '${this.to.name}' by click on '${this.clickSelector!!.name}'"
    }
}

interface INavigationExtractor {
    fun getClickNavigations(): Collection<NavigationTc>
}


class NavigationExtractor(
    private val from: org.xpathqs.core.selector.block.Block,
    private val state: Int = UNDEF_STATE
) : INavigationExtractor {

    override fun getClickNavigations(): Collection<NavigationTc> {
        val res = ArrayList<NavigationTc>()
        from.annotations.filterIsInstance<UI.Nav.PathTo>().forEach {
            if(it.selfPageState == state) {
               // it.contains.forEach {
                    if(it.contain != org.xpathqs.core.selector.block.Block::class) {
                        res.addAll(
                            getClickNavigations(it.contain.objectInstance!!)
                        )
                    }

                //}
            }
        }
        return res + getClickNavigations(from)
    }

    private fun getClickNavigations(from: org.xpathqs.core.selector.block.Block) : Collection<NavigationTc> {
        return from.allInnerSelectors.filter {
            val byClick = it.findAnnotation<UI.Nav.PathTo>()?.byClick
            val blockState = it.findAnyParentAnnotation<UI.Visibility.State>()?.value
            val correctState = if(blockState != null) blockState == state else true

            byClick != null
                    && byClick != org.xpathqs.core.selector.block.Block::class
                    && correctState
        }.map {
            val ann = it.findAnnotation<UI.Nav.PathTo>()!!

            NavigationTc(
                from = from,
                to = ann.byClick.objectInstance!!,
                state = state,
                clickSelector = it
            )
        }
    }
}