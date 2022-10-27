package org.xpathqs.framework

import org.xpathqs.core.reflection.PackageScanner
import org.xpathqs.driver.navigation.Navigator
import org.xpathqs.framework.pom.ThreadLocalNavigator

object UiInitializer {
    private var isInit = ThreadLocal<Boolean>()
    init {
        isInit.set(false)
    }
    fun initNavigations(navigators: Collection<ThreadLocalNavigator>, packagePath: String) {
        synchronized(this) {
            if(isInit.get() == true) return@initNavigations

            navigators.forEach {
                if(it.navigator == null || it.navigator.get() == null) {
                    it.navigator.set(Navigator())
                }
            }

            PackageScanner(packagePath)
                .scan()

            navigators.forEach {
                it.navigator.get().initNavigations()
            }

            isInit.set(true)
        }
    }
}