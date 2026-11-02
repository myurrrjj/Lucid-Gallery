package com.example.chronicle.core.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<Event : ViewEvent, State : ViewState, Effect : ViewSideEffect> :
    ViewModel() {
    private val initialState: State by lazy { createInitialState() }
    abstract fun createInitialState(): State
    private val _uiState: MutableStateFlow<State> = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    private val _effect: Channel<Effect> = Channel()
    val effect: Flow<Effect> = _effect.receiveAsFlow()


    init {
        subscriberEvents()
    }

    private fun subscriberEvents() {
        viewModelScope.launch {
            _event.collect {
                handleEvent(it)

            }
        }
    }

    abstract fun handleEvent(event: Event)

    fun setEvent(event:Event){
        viewModelScope.launch { _event.emit(event) }

    }
    protected fun setState(reduce:State.()->State){
     _uiState.value = uiState.value.reduce()
    }
    protected fun setEffect(builder: () -> Effect) {
        viewModelScope.launch { _effect.send(builder()) }
    }



}