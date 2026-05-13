package com.nitrodropnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nitrodropnative.storage.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeStats(
    val lastTransferSpeed: Long = 0L,
    val totalFilesSent: Int = 0,
    val fastestRecordedSpeed: Long = 0L
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).transferHistoryDao()
    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val last = dao.lastTransfer()
            _stats.value = HomeStats(
                lastTransferSpeed = last?.averageSpeed ?: 0L,
                totalFilesSent = dao.totalFilesSent(),
                fastestRecordedSpeed = dao.fastestSpeed() ?: 0L
            )
        }
    }
}
