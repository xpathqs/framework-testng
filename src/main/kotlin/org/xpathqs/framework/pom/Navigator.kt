package org.xpathqs.framework.pom

import org.xpathqs.driver.navigation.Navigator
import org.xpathqs.framework.pom.IPageNavigator

object Navigator : IPageNavigator {
    override val navigator: Navigator
        get() = DefaultNavigator.navigator.get()
}