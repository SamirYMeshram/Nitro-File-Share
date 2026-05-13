package com.nitrodropnative.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nitrodropnative.ui.screens.DeviceDiscoveryScreen
import com.nitrodropnative.ui.screens.HistoryScreen
import com.nitrodropnative.ui.screens.HomeScreen
import com.nitrodropnative.ui.screens.ReceiveScreen
import com.nitrodropnative.ui.screens.SendScreen
import com.nitrodropnative.ui.screens.SettingsScreen
import com.nitrodropnative.ui.screens.TransferScreen
import com.nitrodropnative.ui.screens.WebTransferScreen
import com.nitrodropnative.viewmodel.DiscoveryViewModel
import com.nitrodropnative.viewmodel.HistoryViewModel
import com.nitrodropnative.viewmodel.HomeViewModel
import com.nitrodropnative.viewmodel.ReceiveViewModel
import com.nitrodropnative.viewmodel.SendViewModel
import com.nitrodropnative.viewmodel.TransferViewModel
import com.nitrodropnative.viewmodel.WebTransferViewModel

object Routes {
    const val HOME = "home"
    const val SEND = "send"
    const val RECEIVE = "receive"
    const val DISCOVERY = "discovery"
    const val TRANSFER = "transfer"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val WEB_TRANSFER = "web_transfer"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val transferViewModel: TransferViewModel = viewModel()
    val sendViewModel: SendViewModel = viewModel()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = vm,
                onSend = { nav.navigate(Routes.SEND) },
                onReceive = { nav.navigate(Routes.RECEIVE) },
                onWebTransfer = { nav.navigate(Routes.WEB_TRANSFER) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SEND) {
            SendScreen(
                viewModel = sendViewModel,
                onBack = { nav.popBackStack() },
                onDiscover = { nav.navigate(Routes.DISCOVERY) },
                onStartTransfer = { uri, ip ->
                    transferViewModel.startSend(uri, ip)
                    nav.navigate(Routes.TRANSFER)
                }
            )
        }
        composable(Routes.RECEIVE) {
            val vm: ReceiveViewModel = viewModel()
            ReceiveScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onStartReceive = {
                    transferViewModel.startReceive()
                    nav.navigate(Routes.TRANSFER)
                }
            )
        }
        composable(Routes.DISCOVERY) {
            val vm: DiscoveryViewModel = viewModel()
            DeviceDiscoveryScreen(
                viewModel = vm,
                sendViewModel = sendViewModel,
                onBack = { nav.popBackStack() },
                onStartTransfer = { uri, connection ->
                    transferViewModel.startSend(uri, connection.host, connection.peerName)
                    nav.navigate(Routes.TRANSFER)
                }
            )
        }
        composable(Routes.TRANSFER) {
            TransferScreen(
                viewModel = transferViewModel,
                onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
            )
        }
        composable(Routes.WEB_TRANSFER) {
            val vm: WebTransferViewModel = viewModel()
            WebTransferScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel()
            HistoryScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
