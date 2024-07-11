package org.xpathqs.framework.util

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.RecordingFileFactory
import org.testcontainers.containers.VncRecordingContainer
import org.xpathqs.framework.base.BaseUiTest
import org.xpathqs.framework.base.Container
import org.xpathqs.log.Log
import org.xpathqs.web.selenium.factory.DriverFactory
import java.io.File
import java.nio.file.Paths

object DriverInitializer {

    fun getCapabilities(): ChromeOptions {
        val options = ChromeOptions()
        options.addArguments(
            "--allow-insecure-localhost",
            "--remote-allow-origins=*",
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

    fun initDocker(caps: ChromeOptions = getCapabilities()) : Pair<WebDriver, BrowserWebDriverContainer<Nothing>> {
        Log.info("initDocker called")

        val dockerBrowser: BrowserWebDriverContainer<Nothing> = Container()
            .withCapabilities(caps)
        dockerBrowser.withRecordingMode(
            BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL,
            File("build"),
            VncRecordingContainer.VncRecordingFormat.MP4
        )
        dockerBrowser.withRecordingFileFactory(
            RecordingFileFactoryImpl()
        )
        dockerBrowser.start()

        return dockerBrowser.webDriver to dockerBrowser
    }

    fun initSelenium(version: String = "latest") : WebDriver {
        Log.info("initSelenium called")
        return DriverFactory(options = getCapabilities(), version = version).create()
    }

    private class RecordingFileFactoryImpl: RecordingFileFactory {
        override fun recordingFileForTest(vncRecordingDirectory: File?, prefix: String?, succeeded: Boolean) =
            Paths.get("${BaseUiTest.commonData.get().curVideoDirPath}/video.mp4").toFile()
    }
}