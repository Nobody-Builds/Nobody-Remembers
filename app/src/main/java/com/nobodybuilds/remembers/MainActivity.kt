package com.nobodybuilds.remembers

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.nobodybuilds.remembers.ui.AppNav
import com.nobodybuilds.remembers.ui.openSession
import com.nobodybuilds.remembers.ui.theme.RemembersTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** Bumped when the notification brings the app back, so we can open the running trail. */
    private val reopened = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val fresh = savedInstanceState == null
        setContent {
            RemembersTheme {
                val nav = rememberNavController()
                AppNav(nav)
                LaunchedEffect(Unit) {
                    reopened.collect { count -> if (count > 0 || fresh) nav.openSession() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        reopened.value++
    }
}
