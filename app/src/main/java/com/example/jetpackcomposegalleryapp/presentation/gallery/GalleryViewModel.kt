package com.example.jetpackcomposegalleryapp.presentation.gallery

//import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.BaseViewModel
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.domain.repository.FaceRepository
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import com.example.jetpackcomposegalleryapp.domain.repository.SettingsRepository
import com.example.jetpackcomposegalleryapp.domain.usecase.GetFilteredMediaUseCase
import com.example.jetpackcomposegalleryapp.domain.usecase.GroupMediaUseCase
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEffect
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryState
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.Album
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.GalleryTab
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.OthersSelection
import com.example.jetpackcomposegalleryapp.worker.FaceIndexingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val mediaRepository: MediaRepository,
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val groupMediaUseCase: GroupMediaUseCase,
    private val getFilteredMediaUseCase: GetFilteredMediaUseCase
) : BaseViewModel<GalleryEvent, GalleryState, GalleryEffect>() {
    private var isFirstSettingsLoad = true

    init {
        observeFavorites()
        observePeopleClusters()
        observeSettings()
    }

    override fun createInitialState(): GalleryState = GalleryState()


    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->

                setState {
                    val newViewMode =
                        if (isFirstSettingsLoad) settings.defaultGalleryViewMode else configState.currentViewMode
                    if (isFirstSettingsLoad && newViewMode != configState.currentViewMode) {
                        regroupMedia(contentState.displayedMediaList, newViewMode)
                    }
                    copy(
                        configState = configState.copy(
                            currentViewMode = newViewMode, autoPlayVideo = settings.autoPlayVideo
                        )
                    ).also { isFirstSettingsLoad = false }
                }
            }
        }
    }


    private fun handleOthersSelectionChanged(selection: OthersSelection) {
        val currentState = uiState.value
        if (currentState.interactionState.othersSelection == selection) return

        val filteredList = getFilteredMediaUseCase(
            masterList = currentState.contentState.masterMediaList,
            favouriteList = currentState.contentState.favouriteMediaList,
            tab = currentState.interactionState.selectedTab,
            othersSelection = selection
        )
        setState {
            copy(
                interactionState = interactionState.copy(
                    othersSelection = selection,
                    selectionMode = false,
                    selectedMediaIds = emptySet()
                ), contentState = contentState.copy(
                    displayedMediaList = filteredList
                )
            )
        }
        regroupMedia(filteredList, currentState.configState.currentViewMode)
    }

    override fun handleEvent(event: GalleryEvent) {
        when (event) {
            is GalleryEvent.LoadMedia -> fetchMedia()
            is GalleryEvent.PermissionResult -> handlePermission(event.isGranted)
            is GalleryEvent.MediaClicked -> navigateToDetail(event.mediaId)
            is GalleryEvent.OnTabSelected -> handleTabSelection(event.tab)
            is GalleryEvent.OnOthersSelectionChanged -> handleOthersSelectionChanged(event.selection)
            is GalleryEvent.OpenPerson -> openPerson(event.cluster)
            GalleryEvent.ClosePerson -> closePerson()
            is GalleryEvent.UpdatePersonName -> updatePersonName(event.clusterId, event.newName)

            is GalleryEvent.PerformMediaAction -> handleMediaAction(
                event.action, event.mediaList
            )

            is GalleryEvent.OpenInfoSheet -> fetchMediaDetails(event.media)
            is GalleryEvent.CloseInfoSheet -> {
                setState { copy(infoSheetState = InfoSheetState.Closed) }
            }

            is GalleryEvent.OpenAlbum -> {
                val newDisplayedList = uiState.value.contentState.masterMediaList.filter {
                    (it.folderName ?: "Unknown") == event.album.name
                }.toPersistentList()
                setState {
                    copy(
                        interactionState = interactionState.copy(openedAlbum = event.album),
                        contentState = contentState.copy(displayedMediaList = newDisplayedList)
                    )
                }
                regroupMedia(newDisplayedList, uiState.value.configState.currentViewMode)
            }

            GalleryEvent.CloseAlbum -> {
                val newDisplayedList = when (uiState.value.interactionState.selectedTab) {
                    GalleryTab.VIDEOS -> uiState.value.contentState.masterMediaList.filter { it.isVideo }
                        .toPersistentList()

                    GalleryTab.OTHERS -> if (uiState.value.interactionState.othersSelection == OthersSelection.FAVOURITES) {
                        uiState.value.contentState.favouriteMediaList
                    } else persistentListOf()

                    else -> uiState.value.contentState.masterMediaList
                }
                setState {
                    copy(
                        interactionState = interactionState.copy(openedAlbum = null),
                        contentState = contentState.copy(displayedMediaList = newDisplayedList)
                    )
                }
                regroupMedia(newDisplayedList, uiState.value.configState.currentViewMode)
            }

            is GalleryEvent.EnterSelectionMode -> {
                setState {
                    copy(
                        interactionState = interactionState.copy(
                            selectionMode = true, selectedMediaIds = emptySet()
                        )
                    )
                }
            }

            GalleryEvent.ExitSelectionMode -> {
                setState {
                    copy(
                        interactionState = interactionState.copy(
                            selectionMode = false, selectedMediaIds = emptySet()
                        )
                    )
                }
            }

            GalleryEvent.SelectAll -> {
                setState {
                    val allIds = when (interactionState.selectedTab) {
                        GalleryTab.ALBUMS -> emptySet()
                        else -> contentState.displayedMediaList.map { it.id }.toSet()
                    }
                    copy(interactionState = interactionState.copy(selectedMediaIds = allIds))
                }
            }

            GalleryEvent.ClearSelection -> {
                setState {
                    copy(interactionState = interactionState.copy(selectedMediaIds = emptySet()))
                }
            }

            is GalleryEvent.ToggleMediaSelection -> {
                setState {
                    val newSet = if (interactionState.selectedMediaIds.contains(event.mediaId)) {
                        interactionState.selectedMediaIds - event.mediaId
                    } else {
                        interactionState.selectedMediaIds + event.mediaId
                    }

                    val keepSelectionMode = newSet.isNotEmpty()
                    copy(
                        interactionState = interactionState.copy(
                            selectedMediaIds = newSet,
                            selectionMode = if (keepSelectionMode) interactionState.selectionMode else false
                        )
                    )
                }
            }

            is GalleryEvent.ChangeViewMode -> {
                setState {
                    copy(configState = configState.copy(currentViewMode = event.viewMode))
                }
                regroupMedia(uiState.value.contentState.displayedMediaList, event.viewMode)
            }

            GalleryEvent.StartFaceIndexing -> startManualFaceIndexing()
            GalleryEvent.OnSettingsClicked -> {
                setEffect { GalleryEffect.NavigateToSettings }
            }

            GalleryEvent.onSearchClicked -> setEffect { GalleryEffect.NavigateToSearch }
            is GalleryEvent.SetSearchMediaList -> {
                setState {
                    copy(
                        contentState = contentState.copy(
                            displayedMediaList = event.results.toImmutableList()
                        )
                    )
                }
            }
            GalleryEvent.ClearSearchMediaList -> {
                val newDisplayedList = when (uiState.value.interactionState.selectedTab) {
                    GalleryTab.VIDEOS -> uiState.value.contentState.masterMediaList.filter { it.isVideo }.toPersistentList()
                    GalleryTab.OTHERS -> if (uiState.value.interactionState.othersSelection == OthersSelection.FAVOURITES) {
                        uiState.value.contentState.favouriteMediaList
                    } else persistentListOf()
                    else -> uiState.value.contentState.masterMediaList
                }
                setState {
                    copy(
                        contentState = contentState.copy(displayedMediaList = newDisplayedList)
                    )
                }
                regroupMedia(newDisplayedList, uiState.value.configState.currentViewMode)
            }


        }
    }

    private fun observePeopleClusters() {
        viewModelScope.launch {
            faceRepository.getAllClusters().collect { clusters ->
                setState { copy(contentState = contentState.copy(peopleClusters = clusters.toImmutableList())) }
            }
        }
    }

    private fun openPerson(cluster: PersonCluster) {
        viewModelScope.launch {
            val mediaIds = faceRepository.getMediaIdsForCluster(cluster.id).toSet()
            val personMedia =
                uiState.value.contentState.masterMediaList.filter { it.id in mediaIds }
                    .toPersistentList()

            setState {
                copy(
                    interactionState = interactionState.copy(openedPersonCluster = cluster),
                    contentState = contentState.copy(displayedMediaList = personMedia)
                )
            }
            regroupMedia(personMedia, uiState.value.configState.currentViewMode)
        }
    }

    private fun closePerson() {
        val newDisplayedList = when (uiState.value.interactionState.selectedTab) {
            GalleryTab.VIDEOS -> uiState.value.contentState.masterMediaList.filter { it.isVideo }
                .toPersistentList()

            GalleryTab.OTHERS -> if (uiState.value.interactionState.othersSelection == OthersSelection.FAVOURITES) {
                uiState.value.contentState.favouriteMediaList
            } else persistentListOf()

            else -> uiState.value.contentState.masterMediaList
        }

        setState {
            copy(
                interactionState = interactionState.copy(openedPersonCluster = null),
                contentState = contentState.copy(displayedMediaList = newDisplayedList)
            )
        }
        regroupMedia(newDisplayedList, uiState.value.configState.currentViewMode)
    }

    private fun updatePersonName(clusterId: Long, newName: String) {
        viewModelScope.launch {
            faceRepository.updateClusterName(clusterId, newName)
        }
    }

    private fun regroupMedia(mediaList: List<MediaAsset>, viewMode: GalleryViewMode) {
        viewModelScope.launch(Dispatchers.Default) {
            val grouped = groupMediaUseCase(mediaList, viewMode)

            setState {
                copy(contentState = contentState.copy(groupedMedia = grouped))
            }
        }
    }

    private fun handleMediaAction(action: DetailAction, explicitMedia: List<MediaAsset>) {
        val targetMedia = explicitMedia.ifEmpty {
            val selectedIds = uiState.value.interactionState.selectedMediaIds
            uiState.value.contentState.masterMediaList.filter { it.id in selectedIds }
        }

        if (targetMedia.isEmpty()) return

        when (action) {
            DetailAction.COPY -> {
                val uris = targetMedia.map { it.uriString }
                setEffect { GalleryEffect.CopyMedia(uris) }
            }

            DetailAction.SHARE -> {
                val uris = targetMedia.map { it.uriString }
                val mimeType = if (targetMedia.size == 1) targetMedia.first().mimeType else "*/*"
                setEffect { GalleryEffect.ShareMedia(uris, mimeType) }
            }

            DetailAction.DELETE -> {
                val uris = targetMedia.map { it.uriString }
                setEffect { GalleryEffect.DeleteMedia(uris) }
            }

            DetailAction.FAVOURITE -> {
                viewModelScope.launch {
                    val favoriteIds = uiState.value.contentState.favoriteMediaIds
                    val allAreFavorites = targetMedia.all { it.id in favoriteIds }

                    targetMedia.forEach { media ->
                        mediaRepository.toggleFavourite(media, !allAreFavorites)
                    }
                    setEvent(GalleryEvent.ExitSelectionMode)
                }
            }

            DetailAction.INFO -> {
                if (targetMedia.size == 1) {
                    fetchMediaDetails(targetMedia.first())
                }
            }

            DetailAction.EDIT -> {
                if (targetMedia.size == 1) {
                    val media = targetMedia.first()
                    setEffect { GalleryEffect.EditMedia(media.uriString, media.mimeType) }
                }
            }
        }
    }

    private fun fetchMediaDetails(media: MediaAsset) {
        setState { copy(infoSheetState = InfoSheetState.Loading(media)) }
        viewModelScope.launch {
            try {
                val details = mediaRepository.getMediaDetails(media.uriString, media.isVideo)
                setState { copy(infoSheetState = InfoSheetState.Success(media, details)) }
            } catch (e: Exception) {
                setState {
                    copy(
                        infoSheetState = InfoSheetState.Error(
                            media, e.message ?: "Unknown Error"
                        )
                    )
                }
            }
        }
    }

    private fun handleTabSelection(tab: GalleryTab) {
        val currentState=  uiState.value
        if (currentState.interactionState.selectedTab == tab) return

        val filteredList = getFilteredMediaUseCase(
            masterList = currentState.contentState.masterMediaList,
            favouriteList = currentState.contentState.favouriteMediaList,
            tab = tab,
            othersSelection = currentState.interactionState.othersSelection
        )
        setState {
            copy(
                interactionState = interactionState.copy(
                    selectedTab = tab, selectionMode = false, selectedMediaIds = emptySet()
                ), contentState = contentState.copy(
                    displayedMediaList = filteredList
                )
            )
        }
        regroupMedia(filteredList, currentState.configState.currentViewMode)
    }

    private fun handlePermission(isGranted: Boolean) {
        setState { copy(configState = configState.copy(hasPermission = isGranted)) }
        if (isGranted) fetchMedia()
        else setState {
            copy(
                configState = configState.copy(
                    isLoading = false, error = "Permission required to display media."
                )
            )
        }
    }

    private fun fetchMedia() {
        if (!uiState.value.configState.hasPermission) {
            setEffect { GalleryEffect.RequestPermission }
            return
        }

        viewModelScope.launch {
            mediaRepository.getAllMedia().onStart {
                    setState {
                        copy(
                            configState = configState.copy(
                                isLoading = true, error = null
                            )
                        )
                    }
                }.catch { exception ->
                    setState {
                        copy(
                            configState = configState.copy(
                                isLoading = false, error = exception.message
                            )
                        )
                    }
                }.collect { media ->
                    val immutableMedia = media.toImmutableList()
                    val processedAlbums = immutableMedia.groupBy { it.folderName ?: "Unknown" }
                        .map { (folderName, items) ->
                            Album(
                                name = folderName, items.size, items.first()
                            )
                        }.sortedBy { it.name }.toImmutableList()

                    setState {
                        val newDisplayedList = when {
                            interactionState.openedAlbum != null -> {
                                immutableMedia.filter {
                                    (it.folderName
                                        ?: "Unknown") == interactionState.openedAlbum?.name
                                }.toImmutableList()
                            }

                            interactionState.openedPersonCluster != null -> {
                                contentState.displayedMediaList
                            }

                            else -> when (interactionState.selectedTab) {
                                GalleryTab.ALL -> immutableMedia
                                GalleryTab.VIDEOS -> immutableMedia.filter { it.isVideo }
                                    .toImmutableList()

                                GalleryTab.ALBUMS -> persistentListOf()
                                GalleryTab.OTHERS -> {
                                    if (interactionState.othersSelection == OthersSelection.FAVOURITES) contentState.favouriteMediaList
                                    else persistentListOf()
                                }
                            }
                        }

                        copy(
                            configState = configState.copy(isLoading = false),
                            contentState = contentState.copy(
                                masterMediaList = immutableMedia,
                                displayedMediaList = newDisplayedList,
                                albums = processedAlbums
                            )
                        )
                    }
                    regroupMedia(
                        uiState.value.contentState.displayedMediaList,
                        uiState.value.configState.currentViewMode
                    )
                }
        }
    }

    private fun navigateToDetail(mediaId: Long) {
        setEffect { GalleryEffect.NavigateToDetail(mediaId) }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            mediaRepository.getFavorites().collect { favourites ->
                val immutableFavourites = favourites.toImmutableList()
                val ids = favourites.map { it.id }.toSet()

                setState {
                    val newDisplayedList = when {
                        interactionState.openedAlbum != null || interactionState.openedPersonCluster != null -> contentState.displayedMediaList
                        interactionState.selectedTab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.FAVOURITES -> immutableFavourites
                        else -> contentState.displayedMediaList
                    }

                    copy(
                        contentState = contentState.copy(
                            favouriteMediaList = immutableFavourites,
                            favoriteMediaIds = ids,
                            displayedMediaList = newDisplayedList
                        )
                    )
                }
                regroupMedia(
                    uiState.value.contentState.displayedMediaList,
                    uiState.value.configState.currentViewMode
                )
            }
        }
    }

    private fun startManualFaceIndexing() {
        val constraints =
            Constraints.Builder()
//                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true).build()

        val workRequest =
            OneTimeWorkRequestBuilder<FaceIndexingWorker>().setConstraints(constraints).build()

        workManager.enqueueUniqueWork(
            "FaceIndexing", ExistingWorkPolicy.KEEP, workRequest
        )
    }
}