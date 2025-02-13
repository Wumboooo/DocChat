package com.example.docchat.ui.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.docchat.ui.ChatSummary

class SummaryViewModel : ViewModel() {
    private val repository = SummaryRepository()
    private val _summaries = MutableLiveData<List<ChatSummary>>()
    val summaries: LiveData<List<ChatSummary>> = _summaries

    fun loadSummaries(globalRole: String) {
        repository.fetchSummaries(globalRole) { summaries ->
            _summaries.value = summaries
        }
    }
}
