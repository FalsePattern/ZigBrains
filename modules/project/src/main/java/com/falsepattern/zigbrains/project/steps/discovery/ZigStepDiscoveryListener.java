package com.falsepattern.zigbrains.project.steps.discovery;

import com.intellij.util.messages.Topic;
import kotlin.Pair;

import java.util.List;

public interface ZigStepDiscoveryListener {
    @Topic.ProjectLevel
    Topic<ZigStepDiscoveryListener> TOPIC = new Topic<>(ZigStepDiscoveryListener.class);

    enum ErrorType {
        MissingToolchain,
        MissingBuildZig,
        GeneralError
    }

    default void preReload() {}
    default void postReload(List<Pair<String, String>> steps) {}
    default void errorReload(ErrorType type) {}
    default void timeoutReload(int seconds) {}
}
