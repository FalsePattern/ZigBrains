package com.falsepattern.zigbrains.debugger.runner.base

import com.falsepattern.zigbrains.project.run.ZigProcessHandler
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class PreLaunchProcessListener(val console: ConsoleView) : ProcessListener {
    var isBuildFailed: Boolean = false
        private set
    lateinit var processHandler: ProcessHandler
        private set

    @Throws(ExecutionException::class)
    suspend fun executeCommandLineWithHook(commandLine: GeneralCommandLine): Boolean {
        return withProgressText(commandLine.commandLineString) {
            val processHandler = ZigProcessHandler(commandLine)
            this@PreLaunchProcessListener.processHandler = processHandler
            hook(processHandler)
            processHandler.startNotify()
            withContext(Dispatchers.Default) {
                processHandler.process.awaitExit()
            }
            runInterruptible {
                processHandler.waitFor()
            }
            return@withProgressText isBuildFailed
        }
    }

    fun hook(handler: ProcessHandler) {
        console.attachToProcess(handler)
        handler.addProcessListener(this)
    }

    override fun processTerminated(event: ProcessEvent) {
        if (event.exitCode != 0) {
            console.print(
                "Process finished with exit code " + event.exitCode,
                ConsoleViewContentType.NORMAL_OUTPUT
            )
            isBuildFailed = true
        } else {
            isBuildFailed = false
            console.print("Build Successful. Starting debug session. \n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
    }
}
