package io.github.rainboom7.cosmetics.ui.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rainboom7.cosmetics.core.collection.CollectionService
import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import io.github.rainboom7.cosmetics.core.model.UsageState
import io.github.rainboom7.cosmetics.data.photo.PhotoStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    data class Loaded(val item: CosmeticItem, val today: LocalDate) : DetailState
}

sealed interface DetailEvent {
    data class AddedToWishlist(val wishlistId: Long) : DetailEvent
    data object MovedToCollection : DetailEvent
    data object Deleted : DetailEvent
}

class DetailViewModel(
    private val id: Long,
    private val service: CollectionService,
    private val photos: PhotoStorage?,
) : ViewModel() {
    val state: StateFlow<DetailState> = service.observeItem(id)
        .map { item -> if (item == null) DetailState.Missing else DetailState.Loaded(item, service.today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    private val eventChannel = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = eventChannel.receiveAsFlow()

    fun setUsage(usage: UsageState) = launch { service.setUsage(id, usage) }

    fun setRemaining(percent: Int) = launch { service.setRemaining(id, percent) }

    fun setOpenedOn(date: LocalDate) = launch {
        val item = (state.value as? DetailState.Loaded)?.item ?: return@launch
        service.save(item.copy(openedOn = date, usage = if (item.usage == UsageState.NEW) UsageState.OPENED else item.usage))
    }

    fun toggleWantBadly() = launch {
        val item = (state.value as? DetailState.Loaded)?.item ?: return@launch
        service.setWantBadly(id, !item.wantBadly)
    }

    /** [uri] from the photo picker; copied into app storage first. */
    fun addPhoto(uri: Uri) = launch {
        val stored = photos?.importFrom(uri) ?: uri.toString()
        service.addPhoto(id, stored)
    }

    fun buyAgain() = launch {
        service.buyAgain(id)?.let { eventChannel.send(DetailEvent.AddedToWishlist(it)) }
    }

    fun markBought() = launch {
        if (service.markBought(id) != null) eventChannel.send(DetailEvent.MovedToCollection)
    }

    fun delete() = launch {
        val deleted = service.delete(id) ?: return@launch
        deleted.photoUris.forEach { photos?.delete(it) }
        eventChannel.send(DetailEvent.Deleted)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
