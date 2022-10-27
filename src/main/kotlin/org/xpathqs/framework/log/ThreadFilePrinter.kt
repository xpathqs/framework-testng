package org.xpathqs.framework.log

import org.xpathqs.log.abstracts.IArgsProcessor
import org.xpathqs.log.abstracts.IBodyProcessor
import org.xpathqs.log.abstracts.IStreamLog
import org.xpathqs.log.message.IMessage
import org.xpathqs.log.printers.args.NoArgsProcessor
import org.xpathqs.log.printers.args.TimeArgsProcessor
import org.xpathqs.log.printers.body.HierarchyBodyProcessor
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class NoStyleBodyMessage : IBodyProcessor {
    override fun processBody(msg: IMessage): String {
        return msg.bodyMessage.toString()
    }
}

class ThreadFilePrinter(
    protected val argsProcessor: IArgsProcessor
        = TimeArgsProcessor(
              NoArgsProcessor()
          ),
    protected val bodyProcessor: IBodyProcessor
        = HierarchyBodyProcessor(
            NoStyleBodyMessage()
        )
) : IStreamLog {
    private val ts = System.currentTimeMillis()
    private val filesMap = HashMap<String, Writer>()

    fun getFileByThread(): Writer {
        val thread = Thread.currentThread()
        val key = thread.id.toString() + ".out"
        return filesMap.getOrPut(
            key
        ) {
            val path = Paths.get(
                "build/logs/$ts"
            )
            Files.createDirectories(path)
            val f = Paths.get("${path.absolutePathString()}/$key").toFile()
            f.createNewFile()
            OutputStreamWriter(
                f.outputStream()
            )
        }
    }

    override fun onLog(msg: IMessage) {
        val out = getFileByThread()
        out.write(
            argsProcessor.processArgs(msg) + " " + bodyProcessor.processBody(msg) + "\n"
        )
        out.flush()
    }
}