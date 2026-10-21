package com.orgbyte.verification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Ink = Color(0xFF202124)
private val Muted = Color(0xFF666A70)
private val Green = Color(0xFF176B45)
private val LightGreen = Color(0xFFEAF4EE)
private val Line = Color(0xFFE2E5E8)
private val Page = Color(0xFFFAFAF8)

enum class VerificationStatus { PENDING, VERIFIED, FAILED }

data class VerificationSession(
    val id: String,
    val status: VerificationStatus
)

interface VerificationApi {
    suspend fun initiate(): String
    suspend fun getStatus(sessionId: String): VerificationStatus
}

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

data class VerificationUiState(
    val sessionId: String? = null,
    val status: VerificationStatus? = null,
    val isStarting: Boolean = false,
    val isPolling: Boolean = false,
    val error: String? = null
)

class VerificationViewModel(
    private val api: VerificationApi = MockVerificationApi()
) : ViewModel() {
    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

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
                        error = exception.message ?: "Unable to check status. Please try again."
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
        _state.value = _state.value.copy(error = null, isPolling = true)
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch { pollStatus(id) }
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

class MainActivity : ComponentActivity() {
    private val viewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VerificationViewModel() as T
            }
        })[VerificationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Green,
                    onPrimary = Color.White,
                    background = Page,
                    surface = Color.White,
                    onSurface = Ink
                )
            ) {
                VerificationApp(viewModel)
            }
        }
    }
}

@Composable
private fun VerificationApp(viewModel: VerificationViewModel) {
    val state by viewModel.state.collectAsState()
    val hasSession = state.sessionId != null

    BackHandler(enabled = hasSession) {
        viewModel.reset()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Page
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "VERIFICATION DEMO",
                color = Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(32.dp))

            if (!hasSession) {
                StartScreen(
                    isLoading = state.isStarting,
                    error = state.error,
                    onStart = viewModel::startVerification
                )
            } else {
                StatusScreen(
                    state = state,
                    onRetry = viewModel::retryPolling,
                    onStartOver = viewModel::reset
                )
            }

            Spacer(Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(40.dp))
            Divider(color = Line)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Local mock API · Status polling every 3 seconds",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StartScreen(
    isLoading: Boolean,
    error: String?,
    onStart: () -> Unit
) {
    Text(
        text = "Verify your identity",
        fontSize = 29.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Start a verification session and follow its status here.",
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = Muted
    )
    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onStart,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.width(18.dp).height(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(Modifier.width(10.dp))
            Text("Starting…")
        } else {
            Text("Start verification")
        }
    }

    if (error != null) {
        Spacer(Modifier.height(16.dp))
        Text(text = error, color = Color(0xFFB3261E), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onStart) {
            Text("Try again")
        }
    }
}

@Composable
private fun StatusScreen(
    state: VerificationUiState,
    onRetry: () -> Unit,
    onStartOver: () -> Unit
) {
    Text(
        text = "Verification status",
        fontSize = 29.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Your session has been created. This page checks its status automatically.",
        fontSize = 15.sp,
        lineHeight = 23.sp,
        color = Muted
    )
    Spacer(Modifier.height(28.dp))

    Divider(color = Line)
    Spacer(Modifier.height(18.dp))
    Text("SESSION ID", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(6.dp))
    Text(state.sessionId.orEmpty(), fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(24.dp))
    Divider(color = Line)
    Spacer(Modifier.height(22.dp))

    Text("CURRENT STATUS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            state.isPolling -> CircularProgressIndicator(
                modifier = Modifier.width(20.dp).height(20.dp),
                strokeWidth = 2.dp,
                color = Green
            )
            state.status == VerificationStatus.VERIFIED -> Text("✓", color = Green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            state.status == VerificationStatus.FAILED -> Text("!", color = Color(0xFFB3261E), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            else -> CircularProgressIndicator(
                modifier = Modifier.width(20.dp).height(20.dp),
                strokeWidth = 2.dp,
                color = Green
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = when (state.status) {
                    VerificationStatus.VERIFIED -> "Verified"
                    VerificationStatus.FAILED -> "Failed"
                    VerificationStatus.PENDING, null -> "Pending"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (state.status) {
                    VerificationStatus.FAILED -> Color(0xFFB3261E)
                    VerificationStatus.VERIFIED -> Green
                    else -> Ink
                }
            )
            Text(
                text = when {
                    state.error != null -> "The latest status check did not complete."
                    state.isPolling -> "Checking again in a few seconds…"
                    state.status == VerificationStatus.VERIFIED -> "The session is complete."
                    state.status == VerificationStatus.FAILED -> "The session could not be verified."
                    else -> "Status updates have stopped."
                },
                fontSize = 14.sp,
                color = Muted
            )
        }
    }

    if (state.error != null) {
        Spacer(Modifier.height(18.dp))
        Text(state.error, color = Color(0xFFB3261E), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Retry status check")
        }
    }

    if (state.status == VerificationStatus.VERIFIED || state.status == VerificationStatus.FAILED || state.error != null) {
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStartOver, modifier = Modifier.fillMaxWidth()) {
            Text("Start over")
        }
    }
}
