/*
 * Copyright 2023-2024 FalsePattern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falsepattern.zigbrains.debugger.dap;

import com.intellij.execution.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.eclipse.lsp4j.debug.BreakpointLocationsArguments;
import org.eclipse.lsp4j.debug.BreakpointLocationsResponse;
import org.eclipse.lsp4j.debug.CancelArguments;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.DataBreakpointInfoArguments;
import org.eclipse.lsp4j.debug.DataBreakpointInfoResponse;
import org.eclipse.lsp4j.debug.DisassembleArguments;
import org.eclipse.lsp4j.debug.DisassembleResponse;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.ExceptionInfoArguments;
import org.eclipse.lsp4j.debug.ExceptionInfoResponse;
import org.eclipse.lsp4j.debug.GotoArguments;
import org.eclipse.lsp4j.debug.GotoTargetsArguments;
import org.eclipse.lsp4j.debug.GotoTargetsResponse;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.LoadedSourcesArguments;
import org.eclipse.lsp4j.debug.LoadedSourcesResponse;
import org.eclipse.lsp4j.debug.ModulesArguments;
import org.eclipse.lsp4j.debug.ModulesResponse;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.ReadMemoryArguments;
import org.eclipse.lsp4j.debug.ReadMemoryResponse;
import org.eclipse.lsp4j.debug.RestartArguments;
import org.eclipse.lsp4j.debug.RestartFrameArguments;
import org.eclipse.lsp4j.debug.ReverseContinueArguments;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetDataBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetDataBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetExpressionArguments;
import org.eclipse.lsp4j.debug.SetExpressionResponse;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetInstructionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetInstructionBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetVariableArguments;
import org.eclipse.lsp4j.debug.SetVariableResponse;
import org.eclipse.lsp4j.debug.SourceArguments;
import org.eclipse.lsp4j.debug.SourceResponse;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepBackArguments;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepInTargetsArguments;
import org.eclipse.lsp4j.debug.StepInTargetsResponse;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.TerminateArguments;
import org.eclipse.lsp4j.debug.TerminateThreadsArguments;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.WriteMemoryArguments;
import org.eclipse.lsp4j.debug.WriteMemoryResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.falsepattern.zigbrains.debugger.dap.Util.get;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public class WrappedDebugServer<T extends IDebugProtocolServer> implements IDebugProtocolServer {
    protected final T server;

    public void cancelNow(CancelArguments args) throws ExecutionException {
        get(cancel(args));
    }

    public Capabilities initializeNow(InitializeRequestArguments args) throws ExecutionException {
        return get(initialize(args));
    }

    public void configurationDoneNow(ConfigurationDoneArguments args) throws ExecutionException {
        get(configurationDone(args));
    }

    public void launchNow(Map<String, Object> args) throws ExecutionException {
        get(launch(args));
    }

    public void attachNow(Map<String, Object> args) throws ExecutionException {
        get(attach(args));
    }

    public void restartNow(RestartArguments args) throws ExecutionException {
        get(restart(args));
    }

    public void disconnectNow(DisconnectArguments args) throws ExecutionException {
        get(disconnect(args));
    }

    public void terminateNow(TerminateArguments args) throws ExecutionException {
        get(terminate(args));
    }

    public BreakpointLocationsResponse breakpointLocationsNow(BreakpointLocationsArguments args) throws ExecutionException {
        return get(breakpointLocations(args));
    }

    public SetBreakpointsResponse setBreakpointsNow(SetBreakpointsArguments args) throws ExecutionException {
        return get(setBreakpoints(args));
    }

    public SetFunctionBreakpointsResponse setFunctionBreakpointsNow(SetFunctionBreakpointsArguments args) throws ExecutionException {
        return get(setFunctionBreakpoints(args));
    }

    public SetExceptionBreakpointsResponse setExceptionBreakpointsNow(SetExceptionBreakpointsArguments args) throws ExecutionException {
        return get(setExceptionBreakpoints(args));
    }

    public DataBreakpointInfoResponse dataBreakpointInfoNow(DataBreakpointInfoArguments args) throws ExecutionException {
        return get(dataBreakpointInfo(args));
    }

    public SetDataBreakpointsResponse setDataBreakpointsNow(SetDataBreakpointsArguments args) throws ExecutionException {
        return get(setDataBreakpoints(args));
    }

    public SetInstructionBreakpointsResponse setInstructionBreakpointsNow(SetInstructionBreakpointsArguments args) throws ExecutionException {
        return get(setInstructionBreakpoints(args));
    }

    public ContinueResponse continueNow(ContinueArguments args) throws ExecutionException {
        return get(continue_(args));
    }

    public void nextNow(NextArguments args) throws ExecutionException {
        get(next(args));
    }

    public void stepInNow(StepInArguments args) throws ExecutionException {
        get(stepIn(args));
    }

    public void stepOutNow(StepOutArguments args) throws ExecutionException {
        get(stepOut(args));
    }

    public void stepBackNow(StepBackArguments args) throws ExecutionException {
        get(stepBack(args));
    }

    public void reverseContinueNow(ReverseContinueArguments args) throws ExecutionException {
        get(reverseContinue(args));
    }

    public void restartFrameNow(RestartFrameArguments args) throws ExecutionException {
        get(restartFrame(args));
    }

    public void gotoNow(GotoArguments args) throws ExecutionException {
        get(goto_(args));
    }

    public void pauseNow(PauseArguments args) throws ExecutionException {
        get(pause(args));
    }

    public StackTraceResponse stackTraceNow(StackTraceArguments args) throws ExecutionException {
        return get(stackTrace(args));
    }

    public ScopesResponse scopesNow(ScopesArguments args) throws ExecutionException {
        return get(scopes(args));
    }

    public VariablesResponse variablesNow(VariablesArguments args) throws ExecutionException {
        return get(variables(args));
    }

    public SetVariableResponse setVariableNow(SetVariableArguments args) throws ExecutionException {
        return get(setVariable(args));
    }

    public SourceResponse sourceNow(SourceArguments args) throws ExecutionException {
        return get(source(args));
    }

    public ThreadsResponse threadsNow() throws ExecutionException {
        return get(threads());
    }

    public void terminateThreadsNow(TerminateThreadsArguments args) throws ExecutionException {
        get(terminateThreads(args));
    }

    public ModulesResponse modulesNow(ModulesArguments args) throws ExecutionException {
        return get(modules(args));
    }

    public LoadedSourcesResponse loadedSourcesNow(LoadedSourcesArguments args) throws ExecutionException {
        return get(loadedSources(args));
    }

    public EvaluateResponse evaluateNow(EvaluateArguments args) throws ExecutionException {
        return get(evaluate(args));
    }

    public SetExpressionResponse setExpressionNow(SetExpressionArguments args) throws ExecutionException {
        return get(setExpression(args));
    }

    public StepInTargetsResponse stepInTargetsNow(StepInTargetsArguments args) throws ExecutionException {
        return get(stepInTargets(args));
    }

    public GotoTargetsResponse gotoTargetsNow(GotoTargetsArguments args) throws ExecutionException {
        return get(gotoTargets(args));
    }

    public CompletionsResponse completionsNow(CompletionsArguments args) throws ExecutionException {
        return get(completions(args));
    }

    public ExceptionInfoResponse exceptionInfoNow(ExceptionInfoArguments args) throws ExecutionException {
        return get(exceptionInfo(args));
    }

    public ReadMemoryResponse readMemoryNow(ReadMemoryArguments args) throws ExecutionException {
        return get(readMemory(args));
    }

    public WriteMemoryResponse writeMemoryNow(WriteMemoryArguments args) throws ExecutionException {
        return get(writeMemory(args));
    }

    public DisassembleResponse disassembleNow(DisassembleArguments args) throws ExecutionException {
        return get(disassemble(args));
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> cancel(CancelArguments args) {
        return server.cancel(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        return server.initialize(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        return server.configurationDone(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        return server.launch(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return server.attach(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> restart(RestartArguments args) {
        return server.restart(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return server.disconnect(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        return server.terminate(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<BreakpointLocationsResponse> breakpointLocations(BreakpointLocationsArguments args) {
        return server.breakpointLocations(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        return server.setBreakpoints(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        return server.setFunctionBreakpoints(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        return server.setExceptionBreakpoints(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<DataBreakpointInfoResponse> dataBreakpointInfo(DataBreakpointInfoArguments args) {
        return server.dataBreakpointInfo(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetDataBreakpointsResponse> setDataBreakpoints(SetDataBreakpointsArguments args) {
        return server.setDataBreakpoints(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetInstructionBreakpointsResponse> setInstructionBreakpoints(SetInstructionBreakpointsArguments args) {
        return server.setInstructionBreakpoints(args);
    }

    @Override
    @JsonRequest("continue")
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        return server.continue_(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> next(NextArguments args) {
        return server.next(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return server.stepIn(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return server.stepOut(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        return server.stepBack(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        return server.reverseContinue(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        return server.restartFrame(args);
    }

    @Override
    @JsonRequest("goto")
    public CompletableFuture<Void> goto_(GotoArguments args) {
        return server.goto_(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> pause(PauseArguments args) {
        return server.pause(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return server.stackTrace(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return server.scopes(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return server.variables(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
        return server.setVariable(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        return server.source(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<ThreadsResponse> threads() {
        return server.threads();
    }

    @Override
    @JsonRequest
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        return server.terminateThreads(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
        return server.modules(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        return server.loadedSources(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return server.evaluate(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<SetExpressionResponse> setExpression(SetExpressionArguments args) {
        return server.setExpression(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        return server.stepInTargets(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        return server.gotoTargets(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return server.completions(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
        return server.exceptionInfo(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<ReadMemoryResponse> readMemory(ReadMemoryArguments args) {
        return server.readMemory(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<WriteMemoryResponse> writeMemory(WriteMemoryArguments args) {
        return server.writeMemory(args);
    }

    @Override
    @JsonRequest
    public CompletableFuture<DisassembleResponse> disassemble(DisassembleArguments args) {
        return server.disassemble(args);
    }
}
