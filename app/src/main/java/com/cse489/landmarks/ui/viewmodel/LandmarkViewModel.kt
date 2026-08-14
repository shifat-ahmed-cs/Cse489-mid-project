package com.cse489.landmarks.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cse489.landmarks.data.local.AppDatabase
import com.cse489.landmarks.data.local.LandmarkEntity
import com.cse489.landmarks.data.local.VisitHistoryEntity
import com.cse489.landmarks.data.repository.LandmarkRepository
import com.cse489.landmarks.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.io.File

enum class SortOption {
    SCORE_DESC,
    SCORE_ASC,
    TITLE_ASC
}

class LandmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LandmarkRepository
    val activeLandmarks: LiveData<List<LandmarkEntity>>
    val deletedLandmarks: LiveData<List<LandmarkEntity>>
    val visitHistory: LiveData<List<VisitHistoryEntity>>

    private val _statusMessage = MutableLiveData<Event<String>>()
    val statusMessage: LiveData<Event<String>> = _statusMessage

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val searchQuery = MutableLiveData<String>("")
    val minScoreFilter = MutableLiveData<Double?>(null)
    val sortOption = MutableLiveData<SortOption>(SortOption.SCORE_DESC)

    val filteredLandmarks: LiveData<List<LandmarkEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LandmarkRepository(database.landmarkDao(), context = application)
        activeLandmarks = repository.activeLandmarks
        deletedLandmarks = repository.deletedLandmarks
        visitHistory = repository.visitHistory

        val mediator = MediatorLiveData<List<LandmarkEntity>>()

        fun applyFilterAndSort() {
            val originalList = activeLandmarks.value ?: emptyList()
            val query = searchQuery.value?.trim()?.lowercase() ?: ""
            val minScore = minScoreFilter.value
            val sort = sortOption.value ?: SortOption.SCORE_DESC

            var list = originalList

            if (query.isNotEmpty()) {
                list = list.filter { it.title.lowercase().contains(query) }
            }

            if (minScore != null) {
                list = list.filter { it.score >= minScore }
            }

            list = when (sort) {
                SortOption.SCORE_DESC -> list.sortedByDescending { it.score }
                SortOption.SCORE_ASC -> list.sortedBy { it.score }
                SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            }

            mediator.value = list
        }

        mediator.addSource(activeLandmarks) { applyFilterAndSort() }
        mediator.addSource(searchQuery) { applyFilterAndSort() }
        mediator.addSource(minScoreFilter) { applyFilterAndSort() }
        mediator.addSource(sortOption) { applyFilterAndSort() }

        filteredLandmarks = mediator

        refreshLandmarks()
    }

    fun refreshLandmarks() {
        viewModelScope.launch {
            _isLoading.value = true
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                val result = repository.refreshLandmarks()
                result.onFailure {
                    _errorMessage.value = Event(it.localizedMessage ?: "Failed to refresh landmarks")
                }
            } else {
                _statusMessage.value = Event("Offline: Showing cached landmarks")
            }
            _isLoading.value = false
        }
    }

    fun visitLandmark(landmarkId: Int, landmarkTitle: String, userLat: Double, userLon: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val isOnline = NetworkUtils.isNetworkAvailable(getApplication())
            val result = repository.visitLandmark(landmarkId, landmarkTitle, userLat, userLon, isOnline)
            result.onSuccess {
                _statusMessage.value = Event(it)
            }.onFailure {
                _errorMessage.value = Event(it.localizedMessage ?: "Visit failed")
            }
            _isLoading.value = false
        }
    }

    fun createLandmark(title: String, lat: Double, lon: Double, imageFile: File?) {
        viewModelScope.launch {
            _isLoading.value = true
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _errorMessage.value = Event("Internet connection required to create landmark!")
                _isLoading.value = false
                return@launch
            }
            val result = repository.createLandmark(title, lat, lon, imageFile)
            result.onSuccess {
                _statusMessage.value = Event(it)
            }.onFailure {
                _errorMessage.value = Event(it.localizedMessage ?: "Failed to create landmark")
            }
            _isLoading.value = false
        }
    }

    fun deleteLandmark(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteLandmark(id)
            result.onSuccess {
                _statusMessage.value = Event(it)
            }.onFailure {
                _errorMessage.value = Event(it.localizedMessage ?: "Failed to delete landmark")
            }
            _isLoading.value = false
        }
    }

    fun restoreLandmark(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.restoreLandmark(id)
            result.onSuccess {
                _statusMessage.value = Event(it)
            }.onFailure {
                _errorMessage.value = Event(it.localizedMessage ?: "Failed to restore landmark")
            }
            _isLoading.value = false
        }
    }
}

// Single event wrapper to avoid double toasts/dialogs
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}
