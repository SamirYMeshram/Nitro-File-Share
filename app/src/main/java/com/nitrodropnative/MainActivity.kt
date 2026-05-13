package com.nitrodropnative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nitrodropnative.core.permissions.PermissionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PermissionManager.requestStartupPermissions(this)
        setContent { NitroDropApp() }
    }
}
