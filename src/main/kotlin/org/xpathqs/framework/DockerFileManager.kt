package org.xpathqs.framework

import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.utility.MountableFile
import java.io.File

class DockerFileManager(
    private val docker: BrowserWebDriverContainer<Nothing>,
    private val files: Set<String>,
    private val baseDir: String = "/home/seluser/"
) {
    private val filesInDocker by lazy {
        files.map {
            val f = File(this::class.java.classLoader.getResource(it).file)
            val dockerPath = baseDir + f.name
            docker.copyFileToContainer(
                MountableFile.forHostPath(f.absolutePath),
                dockerPath
            )
            it to dockerPath
        }.toMap()
    }

    fun resolvePath(key: String) = filesInDocker[key]


}

