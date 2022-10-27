package org.xpathqs.framework.base

import io.qameta.allure.Story
import org.testng.ITest
import org.testng.SkipException
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.framework.log.ScreenshotConfig
import org.xpathqs.framework.pom.*
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.navigation.annotations.UI.Visibility.Companion.UNDEF_STATE
import org.xpathqs.driver.navigation.base.IModelBlock
import org.xpathqs.log.style.StyleFactory
import java.lang.reflect.Method

open class BasePageTest(
    protected val page: Page,
    protected val state: Int = UNDEF_STATE,
    protected val modelBlock: IModelBlock<*>? = page as? IModelBlock<*>,
    startUpPage: Page,
    redirectPage: Page? = null,
    private val allurePrefix: String = "",
    protected val checker: ISelectorCheck = SelectorCheck(),
    protected val extractor: ISelectorExtractor = SelectorExtractor(page, state),

    protected val validationExtractor: IValidationExtractor = ValidationExtractor(modelBlock),
    protected val validationCheck: IValidationCheck = object: ValidationCheck() {
        override val model: IBaseModel?
            get() {
                return modelBlock?.invoke()
            }
    },
    protected var stateHolder: IPageStateHolder? = null,

    protected val navigationExtractor: INavigationExtractor = NavigationExtractor(page, state),
    protected val navigationCheck: INavigationCheck = NavigationCheck(),

    afterDriverCreated: (BaseUiTest.()->Unit)? = null,
    callbacks: UITestCallbacks = object : UITestCallbacks {},
    navigators: Collection<ThreadLocalNavigator> = listOf(DefaultNavigator),
    navigator: IPageNavigator = Navigator
) : BaseUiTest(
    startUpPage = startUpPage,
    redirectPage = redirectPage,
    afterDriverCreated = afterDriverCreated,
    navigators = navigators,
    callbacks = callbacks,
    navigator = navigator
), ITest {

    override fun precondition() {
        if(validationCheck.model != null) {
            stateHolder = PageStateHolder(validationCheck.model!!.apply { default() })
            (validationCheck as ValidationCheck).stateHolder = stateHolder
        }
    }

    @Story("Elements/Static")
    @Test(dataProvider = "getStatic")
    fun testStatic(sel: BaseSelector) {
        checker.checkSelector(sel)
    }

    @Story("Elements/Dynamic")
    @Test(dataProvider = "getDynamic")
    fun testDynamic(sel: BaseSelector) {
        checker.checkSelector(sel)
    }

    @Story("Validations")
    @Test(dataProvider = "getValidations")
    fun testValidations(tc: ValidationTc) {
        validationCheck.checkValidation(tc)
    }

    @Story("Navigations")
    @Test(dataProvider = "getClickNavigation")
    @ScreenshotConfig(actionInWhen = true, beforeThen = true)
    fun testClickNavigations(tc: NavigationTc) {
        navigationCheck.checkNavigation(tc)
    }

    @DataProvider
    fun getStatic() = extractor.staticSelectors.toTypedArray()

    @DataProvider
    fun getDynamic() = extractor.dynamicSelectors.toTypedArray()

    @DataProvider
    fun getValidations() = validationExtractor.validations.toTypedArray()

    @DataProvider
    fun getClickNavigation() = navigationExtractor.getClickNavigations().toTypedArray()

    companion object {
        val testCaseName = ThreadLocal<String>()
    }

    override fun getTestName(): String {
        return testCaseName.get() ?: ""
    }

    @BeforeMethod(alwaysRun = true)
    open fun setTestName(method: Method, args: Array<Any?>?) {
        if(args?.isNotEmpty() == true) {
            val arg = args[0]
            when(arg) {
                is BaseSelector -> {
                    testCaseName.set(arg.name)
                }
                is ValidationTc -> {
                    val title = "Validation for field '${arg.v.prop.name}' with type '${arg.rule}'"
                    testCaseName.set(title)
                    Log.tag(
                        StyleFactory.testTitle("                    $title                    "), "title"
                    )
                }
                is NavigationTc -> {
                    testCaseName.set(arg.getName())
                }
                else -> {
                    testCaseName.set(method.name)
                }
            }
        } else {
            testCaseName.set(method.name)
        }
    }

    @BeforeClass
    fun navigate() {
        try {
            page.navigate(state)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SkipException("Navigation was not completed")
        }
    }
}