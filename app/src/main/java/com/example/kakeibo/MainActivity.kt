package com.example.kakeibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.kakeibo.ui.KakeiboApp
import com.example.kakeibo.ui.theme.KakeiboTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KakeiboApplication
        setContent {
            KakeiboTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KakeiboApp(app.repository)
                }
            }
        }
    }
}
