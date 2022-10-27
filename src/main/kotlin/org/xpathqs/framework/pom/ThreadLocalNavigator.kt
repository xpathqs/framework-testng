package org.xpathqs.framework.pom

import org.xpathqs.driver.navigation.Navigator

interface ThreadLocalNavigator {
    val navigator: ThreadLocal<Navigator>
}

/*
object IbNavigator: ThreadLocalNavigator {
    override val navigator: ThreadLocal<Navigator> = ThreadLocal.withInitial { Navigator() }
}*/
