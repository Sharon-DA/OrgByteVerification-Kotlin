package com.orgbyte.verification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orgbyte.verification.data.MockVerificationApi
import com.orgbyte.verification.data.VerificationApi
import com.orgbyte.verification.model.VerificationStatus
import com.orgbyte.verification.model.VerificationUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VerificationViewModel(
    private val api: VerificationApi = MockVerificationApi()
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun startVerification() {
        if (_state.value.isStarting || _state.value.isPolling) return

        pollingJob?.cancel()
        _state.value = VerificationUiState(isStarting = true)

        viewModelScope.launch {
            try {
                val id = api.initiate()

                _state.value = VerificationUiState(
                    sessionId = id,
                    status = VerificationStatus.PENDING,
                    isPolling = true
                )

                pollStatus(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _state.value = VerificationUiState(
                    error = exception.message ?: "Could not start verification."
                )
            }
        }
    }

    private suspend fun pollStatus(sessionId: String) {
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val status = api.getStatus(sessionId)

                    _state.value = _state.value.copy(
                        status = status,
                        isPolling = status == VerificationStatus.PENDING,
                        error = null
                    )

                    if (status != VerificationStatus.PENDING) break

                    delay(3_000)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    _state.value = _state.value.copy(
                        isPolling = false,
                        error = exception.message
                            ?: "Unable to check status. Please try again."
                    )
                    break
                }
            }
        }

        pollingJob?.join()
    }

    fun retryPolling() {
        val id = _state.value.sessionId ?: return
        if (_state.value.isPolling) return

        _state.value = _state.value.copy(
            error = null,
            isPolling = true
        )

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            pollStatus(id)
        }
    }

    fun reset() {
        pollingJob?.cancel()
        _state.value = VerificationUiState()
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
