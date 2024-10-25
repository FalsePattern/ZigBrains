package com.falsepattern.zigbrains.debugger.runner.base;

import com.falsepattern.zigbrains.project.runconfig.ZigProcessHandler;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class PreLaunchProcessListener implements ProcessListener {
    private final ConsoleView console;
    private boolean buildFailed = false;
    private ProcessHandler processHandler;

    public boolean executeCommandLineWithHook(GeneralCommandLine commandLine) throws ExecutionException {
        processHandler = new ZigProcessHandler(commandLine);
        hook(processHandler);
        processHandler.startNotify();
        processHandler.waitFor();
        return buildFailed;
    }

    public void hook(ProcessHandler handler) {
        console.attachToProcess(handler);
        handler.addProcessListener(this);
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        if (event.getExitCode() != 0) {
            console.print("Process finished with exit code " + event.getExitCode(),
                          ConsoleViewContentType.NORMAL_OUTPUT);
            buildFailed = true;
        } else {
            buildFailed = false;
            console.print("Build Successful. Starting debug session. \n", ConsoleViewContentType.NORMAL_OUTPUT);
        }
    }
}
