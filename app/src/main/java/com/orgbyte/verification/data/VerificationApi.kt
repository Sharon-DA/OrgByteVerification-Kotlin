
package com.orgbyte.verification.data

import com.orgbyte.verification.model.VerificationStatus

interface VerificationApi {
    suspend fun initiate(): String
    suspend fun getStatus(sessionId: String): VerificationStatus
}
