package com.portfolio.splitsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.portfolio.splitsync.ui.navigation.SplitSyncNavHost
import com.portfolio.splitsync.ui.theme.SplitSyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitSyncTheme(darkTheme = true) {
                SplitSyncNavHost()
            }
        }
    }
}
