package org.xpathqs.framework.widgets

import org.xpathqs.core.selector.base.BaseSelector

interface ISelectorNav {
    fun navigateDirectly(to: BaseSelector)
}