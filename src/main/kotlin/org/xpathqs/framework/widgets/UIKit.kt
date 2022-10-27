package org.xpathqs.framework.widgets

import org.xpathqs.core.selector.extensions.contains
import org.xpathqs.web.factory.HTML

object UIKit {
    fun input(label: String) = ValidationInput(
        base = HTML.div(clsContains = "form-group") contains HTML.label(text = label),
        input = HTML.input(),
        lblError = HTML.div(cls = "invalid-feedback")
    )
}