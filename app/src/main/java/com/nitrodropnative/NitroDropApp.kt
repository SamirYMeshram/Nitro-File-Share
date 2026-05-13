package com.nitrodropnative

import androidx.compose.runtime.Composable
import com.nitrodropnative.ui.navigation.AppNavigation
import com.nitrodropnative.ui.theme.NitroDropTheme

@Composable
fun NitroDropApp() {
    NitroDropTheme {
        AppNavigation()
    }
}
