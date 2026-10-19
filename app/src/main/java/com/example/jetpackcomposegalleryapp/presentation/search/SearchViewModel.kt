package com.example.jetpackcomposegalleryapp.presentation.search

import androidx.lifecycle.viewModelScope
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.BaseViewModel
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import com.example.jetpackcomposegalleryapp.domain.repository.SearchRepository
import com.example.jetpackcomposegalleryapp.domain.usecase.GroupMediaUseCase
import com.example.jetpackcomposegalleryapp.presentation.search.contract.SearchEffect
import com.example.jetpackcomposegalleryapp.presentation.search.contract.SearchEvent
import com.example.jetpackcomposegalleryapp.presentation.search.contract.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val mediaRepository: MediaRepository,
    private val groupMediaUseCase: GroupMediaUseCase
) : BaseViewModel<SearchEvent, SearchState, SearchEffect>() {

    private val searchQueryFlow = MutableStateFlow("")
    private var cachedMediaList: List<MediaAsset> = emptyList()

    init {
        initializeSearchIndex()
        observeSearchExecution()
    }

    private fun initializeSearchIndex() {
        viewModelScope.launch {
            val media = mediaRepository.getAllMedia().first()
            val favs = mediaRepository.getFavorites().first().map { it.id }.toSet()
            cachedMediaList = media
            searchRepository.syncSearchIndex(media, favs).collectLatest { progress ->
                setState {
                    copy(
                        indexingProgress = progress,
                        isIndexing = progress < 1f
                    )
                }
            }
        }
    }

    override fun createInitialState(): SearchState = SearchState()

    override fun handleEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChanged -> {
                setState { copy(query = event.query) }
                searchQueryFlow.value = event.query
            }
            SearchEvent.ClearSearch -> {
                setState { copy(query = "", groupedResults = persistentMapOf(), isSearching = false) }
                searchQueryFlow.value = ""
            }
            is SearchEvent.OnMediaClicked -> setEffect { SearchEffect.NavigateToDetail(event.mediaId) }
            SearchEvent.OnBackClicked -> setEffect { SearchEffect.NavigateBack }
            is SearchEvent.ChangeViewMode -> {
                setState { copy(currentViewMode = event.viewMode) }
                regroupResults()
            }
        }
    }

    private fun observeSearchExecution() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        setState { copy(groupedResults = persistentMapOf(), isSearching = false) }
                        return@collectLatest
                    }

                    setState { copy(isSearching = true) }

                    val matchedMedia = try {
                        val matchedIds = searchRepository.searchMediaIds(query).toSet()
                        cachedMediaList.filter { it.id in matchedIds }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val grouped = groupMediaUseCase(matchedMedia, uiState.value.currentViewMode)
                    setState {
                        copy(
                            groupedResults = grouped,
                            isSearching = false
                        )
                    }
                }
        }
    }

    private fun regroupResults() {
        val currentResults = uiState.value.groupedResults.values.flatten()
        if (currentResults.isEmpty()) return
        val newGrouped = groupMediaUseCase(currentResults, uiState.value.currentViewMode)
        setState { copy(groupedResults = newGrouped) }
    }
}