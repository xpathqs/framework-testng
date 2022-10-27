package org.xpathqs.framework.pom

import org.xpathqs.driver.navigation.Navigator
import org.xpathqs.framework.pom.ThreadLocalNavigator

object DefaultNavigator: ThreadLocalNavigator {
    override val navigator = ThreadLocal<Navigator>()
}