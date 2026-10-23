package com.orgbyte.verification.model

enum class VerificationStatus {
    PENDING,
    VERIFIED,
    FAILED
}

data class VerificationSession(
    val id: String,
    val status: VerificationStatus
)

data class VerificationUiState(
    val sessionId: String? = null,
    val status: VerificationStatus? = null,
    val isStarting: Boolean = false,
    val isPolling: Boolean = false,
    val error: String? = null
)
