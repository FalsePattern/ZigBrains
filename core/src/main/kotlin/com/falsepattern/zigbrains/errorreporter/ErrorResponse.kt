package com.falsepattern.zigbrains.errorreporter

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val url: String?,
    val linkText: String?,
    val status: Status,
) {
    @Serializable
    enum class Status {
        New,
        Duplicate,
        OldVersion,
    }
}
