package com.nitrodropnative.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nitrodropnative.storage.FileReader
import com.nitrodropnative.storage.SelectedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SendViewModel(application: Application) : AndroidViewModel(application) {
    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    private val _receiverIp = MutableStateFlow("")
    val receiverIp: StateFlow<String> = _receiverIp.asStateFlow()

    fun setReceiverIp(value: String) { _receiverIp.value = value.trim() }

    fun setUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedFiles.value = uris.map { FileReader.info(getApplication(), it) }
        }
    }

    fun clear() { _selectedFiles.value = emptyList() }
}
