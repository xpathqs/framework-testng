package org.xpathqs.framework.pom

import io.qameta.allure.Allure
import org.testng.SkipException
import org.xpathqs.core.selector.block.Block
import org.xpathqs.framework.getStaticSelectorsWithState
import org.xpathqs.framework.бытьВидимым
import org.xpathqs.framework.должен
import org.xpathqs.driver.extensions.click
import org.xpathqs.driver.extensions.isHidden
import org.xpathqs.driver.extensions.waitForAllVisible
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.navigation.base.ILoadableDelegate
import org.xpathqs.gwt.WHEN
import org.xpathqs.log.style.StyleFactory
import java.time.Duration

interface INavigationCheck {
    fun checkNavigation(nav: NavigationTc)
}

class NavigationCheck : INavigationCheck {

    override fun checkNavigation(nav: NavigationTc) {
        val name = nav.getName()
        Allure.getLifecycle().updateTestCase {
            it.name = name
        }

        Log.tag(
            StyleFactory.testTitle("                    $name                   "), "title"
        )



        WHEN("Click on '${nav.clickSelector!!.name}'") {
            try {
                nav.clickSelector.click()
            } catch (e: Exception) {
                if(nav.clickSelector.isHidden) {
                    throw SkipException("Click Selector is not visible")
                }
            }

            val loadAndCheck = (nav.to as Block).getStaticSelectorsWithState(nav.state)

            Log.action("Waiting for the load") {
                if(nav.to is ILoadableDelegate) {
                    nav.to.waitForLoad(Duration.ofSeconds(5))
                } else {
                    loadAndCheck.waitForAllVisible(Duration.ofSeconds(5))
                }
            }
            loadAndCheck
        }.THEN("'${nav.to.name}' should be present") {
            actual.forEach {
                it должен бытьВидимым
            }
        }
    }

}