package com.utilitylab.focussense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.utilitylab.focussense.ui.FocusSenseRoot
import com.utilitylab.focussense.ui.theme.FocusSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusSenseTheme {
                FocusSenseRoot()
            }
        }
    }
}
