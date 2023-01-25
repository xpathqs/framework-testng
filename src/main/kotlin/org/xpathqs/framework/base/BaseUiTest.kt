package org.xpathqs.framework.base

import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.RecordingFileFactory
import org.testcontainers.containers.VncRecordingContainer
import org.testcontainers.lifecycle.TestDescription
import org.testng.annotations.*
import org.xpathqs.core.selector.base.BaseSelector
import org.xpathqs.core.selector.selector.Selector
import org.xpathqs.framework.DockerFileManager
import org.xpathqs.framework.log.*
import org.xpathqs.driver.actions.InputFileAction
import org.xpathqs.driver.executor.ActionExecMap
import org.xpathqs.driver.executor.Decorator
import org.xpathqs.driver.executor.IExecutor
import org.xpathqs.driver.i18n.I18nHelper
import org.xpathqs.driver.log.Log
import org.xpathqs.driver.navigation.NavExecutor
import org.xpathqs.driver.navigation.base.IGlobalState
import org.xpathqs.driver.navigation.base.NoGlobalState
import org.xpathqs.framework.UiInitializer
import org.xpathqs.framework.pom.*
import org.xpathqs.gwt.GIVEN
import org.xpathqs.log.BaseLogger
import org.xpathqs.web.selenium.constants.Global
import org.xpathqs.web.selenium.executor.ExecutorFactory
import org.xpathqs.web.selenium.executor.SeleniumBaseExecutor
import org.xpathqs.web.selenium.factory.DriverFactory
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlin.concurrent.getOrSet

open class DockerExecutor(origin: IExecutor): Decorator(origin) {
    override val actions = ActionExecMap().apply {
        set(InputFileAction("", Selector()).name) {
            executeAction(it as InputFileAction)
        }
    }

    fun executeAction(action: InputFileAction) {
        if(BaseUiTest.commonData.get().useDocker) {
            action.text = BaseUiTest.commonData.get().dfm.resolvePath(action.text)!!
            origin.execute(action)
        } else {
            val path = this::class.java.classLoader.getResource(action.text).file
            //для винды добавляется лишний '/' вначале - его удаляем
            action.text = if(System.getProperty("os.name").lowercase().contains("windows")) path.drop(1) else path
            origin.execute(action)
        }
    }
}

class CommonData {
//    var LOG_PATH: String = ""
    lateinit var driver: WebDriver
    lateinit var docker: BrowserWebDriverContainer<Nothing>
    lateinit var dfm: DockerFileManager
    lateinit var executor: NavExecutor

    var beforeTestCalled = false
    var curVideoDirPath = ""
    var startTs = System.currentTimeMillis()

    var useDocker = false
}

data class XpathQsConfig(
    val packageWithPages: String = "",
    val disableAllScreenshots: Boolean = true,
    val enableScreenshots: Boolean = false,
    val webdriverVersion: String = "latest",
    val lang: String = "ru"
)

interface UITestCallbacks {
    fun beforeDriverInitializedCallback() {}
}

//@Epic("UI-Tests")
open class BaseUiTest(
    val startUpPage: Page? = null,
    val redirectPage: Page? = null,
    val logger: BaseLogger = UiLogger,
    protected val afterDriverCreated: (BaseUiTest.()->Unit)? = null,
    protected val navigator: IPageNavigator = Navigator,
    protected val callbacks: UITestCallbacks = object : UITestCallbacks {},
    protected val navigators: Collection<ThreadLocalNavigator> = listOf(DefaultNavigator),
    private val globalState: IGlobalState = NoGlobalState
) {

    protected open fun defaultSetup() {
        AllureLogCallback.config.set(GWTConfigData())

        val cd = CommonData()
        commonData.set(cd)

        val docker = System.getenv("useDocker") ?: "false"

        if(docker == "true") {
            cd.useDocker = true
        }

        System.setProperty("i18n", config.lang)
        I18nHelper

        initLog(logger)
        UiInitializer.initNavigations(
            navigators,
            config.packageWithPages
        )

        callbacks.beforeDriverInitializedCallback()
        initDriver()
        commonData.get().driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS)

        SeleniumBaseExecutor.enableScreenshots = false
        SeleniumBaseExecutor.disableAllScreenshots = config.disableAllScreenshots

        var executor: IExecutor = NavExecutor(
            origin = ExecutorFactory(cd.driver).getCached(),
            navigator = navigator.navigator,
            globalState = globalState
        )
        cd.executor = executor as NavExecutor
        executor = DockerExecutor(executor)
        Global.init(executor)

        if(startUpPage != null) {
            Log.action("Open start page with address: ${startUpPage.url}") {
                //cd.driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS)
                try {
                    startUpPage.open()
                    startUpPage.waitForLoad(Duration.ofSeconds(10))
                } catch (e: Exception) {
                    Log.error("startUpPage page was not loaded for 10 seconds")
                } finally {
                    cd.driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS)
                }
            }
        }

        afterDriverCreated?.invoke(this)
    }



    private fun initDriver() {
        if(commonData.get().useDocker) {
            initDocker()
        } else {
            initSelenium(readProperties().webdriverVersion)
        }
    }

    private fun getCapabilities(): ChromeOptions {
        val options = ChromeOptions()
        options.addArguments(
            "--allow-insecure-localhost",
            "--safebrowsing-disable-extension-blacklist",
            "--safebrowsing-disable-download-protection",
        )

        options.setCapability(ChromeOptions.CAPABILITY, options)
        options.setCapability("acceptInsecureCerts", true)

        val chromePrefs = HashMap<String, Any>()
        chromePrefs["profile.default_content_settings.popups"] = 0
        chromePrefs["download.default_directory"] = "build"
        chromePrefs["safebrowsing.enabled"] = "true"

        options.setExperimentalOption("prefs", chromePrefs)

        return options
    }

    private fun initDocker() {
        Log.info("initDocker called")
        val chrome = getCapabilities()
        val dockerBrowser: BrowserWebDriverContainer<Nothing> = Container()
            .withCapabilities(chrome)
        dockerBrowser.withRecordingMode(
            BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL,
            File("build"),
            VncRecordingContainer.VncRecordingFormat.MP4
        )
        dockerBrowser.withRecordingFileFactory(
            RecordingFileFactoryImpl()
        )
        dockerBrowser.start()

        val driver = dockerBrowser.webDriver
        commonData.get().driver = driver
        commonData.get().docker = dockerBrowser

        SubtitleDecorator.commonData.getOrSet { org.xpathqs.framework.log.CommonData() }
            .dockerStart = System.currentTimeMillis()

        /*commonData.get().dfm = DockerFileManager(
            commonData.get().docker,
            setOf(
                FileResources.PDF_DOC,
                FileResources.TXT_DOC,
            )
        )*/
    }

    private fun initSelenium(version: String = "latest") {
        Log.info("initSelenium called")
        val driver = DriverFactory(options = getCapabilities(), version = version).create()
        commonData.get().driver = driver
    }

    private fun startUpInit() {
        if(commonData.get() == null) {
            defaultSetup()
        } else {
            commonData.get().driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS)
            try {
                redirectPage?.open()
            } catch (e: Exception) {
                commonData.get().driver.navigate().refresh()
                Log.error("Redirect page was not loaded for 10 seconds")
            } finally {
                commonData.get().driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS)
            }
        }
        createVideoRecord()
    }

    @BeforeTest
    protected fun beforeTest() {
        startUpInit()
        commonData.get().beforeTestCalled = true
    }

    @BeforeClass
    protected fun beforeAll() {
        if(commonData.get()?.beforeTestCalled == false) {
            startUpInit()
        }
        checkPrecondition()
    }

    @BeforeMethod
    fun initGwtConfig(method: Method) {
        var clsSet = false
        var methodSet = false
        val cls = method.javaClass.declaredFields.find { it.name == "clazz" }
        cls?.let {
            it.isAccessible = true
            val clazz = it.get(method) as Class<*>

            val classAnnotation = clazz.declaredAnnotations.find {
                it.annotationClass.java == ScreenshotConfig::class.java
            } as? ScreenshotConfig

            if(classAnnotation != null) {
                AllureLogCallback.config.set(GWTConfigData(classAnnotation))
                clsSet = true
            }
        }
        val methodAnnotation = method.declaredAnnotations.find {
            it.annotationClass.java == ScreenshotConfig::class.java
        } as? ScreenshotConfig

        if(methodAnnotation != null) {
            AllureLogCallback.config.set(GWTConfigData(methodAnnotation))
            methodSet = true
        }

        if(!clsSet && !methodSet) {
            AllureLogCallback.config.set(GWTConfigData())
        }
    }

    protected fun checkPrecondition() {
        if(!preconditionCalled) {
            precondition()
            preconditionCalled = true
        }
    }

    protected fun createVideoRecord() {
        if(commonData.get().useDocker) {
            commonData.get().curVideoDirPath = "build/videos/${commonData.get().startTs}_${(0..10000).random()}/${this.javaClass.simpleName}"
            val path = Paths.get(commonData.get().curVideoDirPath)
            Files.createDirectories(path)

            UiLogger.updatePath(commonData.get().curVideoDirPath)
        }
    }

    fun tearDownInit() {
        Log.info("call tearDownInit")
        commonData.get().driver.quit()
        if(commonData.get().useDocker) {
            Log.info("stopping docker...")
            commonData.get().docker.afterTest(TestDi(), Optional.of(Throwable("")))
            commonData.get().docker.stop()
            Log.info("docker stopped")
        }
        UiLogger.flush()
        commonData.set(null)
    }

    @AfterTest(alwaysRun = true)
    protected fun afterTest() {
        tearDownInit()
    }

    @AfterClass(alwaysRun = true)
    protected fun afterAll() {
        if(commonData.get()?.beforeTestCalled == false) {
            tearDownInit()
        }
    }

    protected fun initLog(logger: BaseLogger) {
        Log.log = logger
        GIVEN.log = logger
        GIVEN.logEvaluator = LogEvaluate()
    }

    companion object {
        val commonData = ThreadLocal<CommonData>()
        val currentPage: Page
            get() = commonData.get().executor.navigator.currentPage as Page

        val driver: ChromeDriver
            get() = commonData.get().driver as ChromeDriver

        fun takeScreenshot(): ByteArray {
            return (commonData.get().driver as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
        }

        val config = readProperties()

        private fun readProperties() : XpathQsConfig {
            try {
                val p = Properties()
                p.load(
                    this::class.java.classLoader.getResourceAsStream("xpathqs.properties")
                )
                return XpathQsConfig(
                    packageWithPages = p.getProperty("packageWithPages") ?: "",
                    disableAllScreenshots = p.getProperty("disableAllScreenshots").toBooleanStrictOrNull() ?: true,
                    enableScreenshots = p.getProperty("enableScreenshots").toBooleanStrictOrNull() ?: false,
                    webdriverVersion = p.getProperty("webdriverVersion"),
                    lang = p.getProperty("lang") ?: "en"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return XpathQsConfig()
        }
    }

    private class TestDi: TestDescription {
        override fun getTestId() = ""
        override fun getFilesystemFriendlyName() = "/build/"
    }

    private class RecordingFileFactoryImpl: RecordingFileFactory {
        override fun recordingFileForTest(vncRecordingDirectory: File?, prefix: String?, succeeded: Boolean) =
             Paths.get("${commonData.get().curVideoDirPath}/video.mp4").toFile()
    }

    val BaseSelector.webElement: WebElement
        get() = commonData.get().driver.findElement(
            By.xpath(this.toXpath())
        )


    open fun precondition() {}

    private var preconditionCalled = false
}
