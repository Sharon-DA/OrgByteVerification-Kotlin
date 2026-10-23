package com.orgbyte.verification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgbyte.verification.ui.VerificationApp
import com.orgbyte.verification.ui.VerificationViewModel

private val Green = Color(0xFF176B45)
private val Ink = Color(0xFF202124)
private val Page = Color(0xFFFAFAF8)

class MainActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return VerificationViewModel() as T
                }
            }
        )[VerificationViewModel::class.java]
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
