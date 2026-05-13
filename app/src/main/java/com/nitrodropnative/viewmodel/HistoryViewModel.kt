package com.nitrodropnative.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nitrodropnative.storage.AppDatabase
import com.nitrodropnative.storage.TransferHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    val history: Flow<List<TransferHistoryEntity>> = AppDatabase.get(application).transferHistoryDao().observeHistory()
}
