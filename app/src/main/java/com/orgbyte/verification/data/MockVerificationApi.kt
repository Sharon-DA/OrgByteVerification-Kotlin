package com.orgbyte.verification.data

import com.orgbyte.verification.model.VerificationStatus
import kotlinx.coroutines.delay

/**
 * Local mock API. It behaves like a remote API but needs no server.
 * First two status checks are PENDING; the third is VERIFIED.
 */
class MockVerificationApi : VerificationApi {
    private var checks = 0

    override suspend fun initiate(): String {
        delay(500)
        checks = 0
        return "ver_${System.currentTimeMillis().toString().takeLast(8)}"
    }

    override suspend fun getStatus(sessionId: String): VerificationStatus {
        delay(350)
        checks += 1

        return when {
            checks >= 3 -> VerificationStatus.VERIFIED
            else -> VerificationStatus.PENDING
        }
    }
}
