package com.falsepattern.zigbrains.errorreporter

import kotlinx.serialization.Serializable

@Serializable
data class ErrorData(
    val description: String?,
    val pluginName: String?,
    val pluginVersion: String?,
    val osName: String,
    val javaVersion: String,
    val javaVmVendor: String,
    val appName: String,
    val appFullName: String,
    val appVersionName: String,
    val isEAP: Boolean,
    val appBuild: String,
    val appVersion: String,
    val lastAction: String?,
    val errorMessage: String?,
    val stackTrace: StackTrace?,
    val attachments: List<Attachment>
) {
    @Serializable
    data class StackTrace(
        val text: String,
        val elements: List<StackTraceElement>
    )

    @Serializable
    data class StackTraceElement(
        val className: String,
        val file: String?,
        val line: Int,
        val text: String,
    )
    @Serializable
    data class Attachment(
        val name: String,
        val displayText: String,
        val encodedBytes: String
    )
}