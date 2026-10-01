package com.contadora.dinero

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MoneyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Storage(app)
    private var persistDraftJob: Job? = null

    var denoms by mutableStateOf(store.loadDenoms() ?: defaultDenominations().also(store::saveDenoms))
        private set
    var history by mutableStateOf(store.loadHistory())
        private set
    var draft by mutableStateOf(store.loadDraft())
        private set
    var symbol by mutableStateOf(store.loadSymbol())
        private set
    var countName by mutableStateOf("")

    var sortedDenoms by mutableStateOf(emptyList<Denomination>())
        private set
    var visibleDenoms by mutableStateOf(emptyList<Denomination>())
        private set
    var currentItems by mutableStateOf(emptyList<CountItem>())
        private set

    init {
        rebuildDenomCaches()
        rebuildCurrentItems()
    }

    private fun rebuildDenomCaches() {
        sortedDenoms = denoms.sortedWith(compareByDescending<Denomination> { it.cents }.thenBy { it.type })
        visibleDenoms = sortedDenoms.filter { it.visible }
    }

    private fun rebuildCurrentItems() {
        currentItems = visibleDenoms.mapNotNull { d ->
            draft[d.id]?.takeIf { it > 0 }?.let { CountItem(d.cents, d.type, it) }
        }
    }

    private fun persistDraftDebounced() {
        persistDraftJob?.cancel()
        persistDraftJob = viewModelScope.launch {
            delay(220)
            store.saveDraft(draft)
        }
    }

    fun setQty(id: String, qty: Int) {
        draft = if (qty > 0) draft + (id to qty) else draft - id
        rebuildCurrentItems()
        persistDraftDebounced()
    }

    fun clearCount() {
        draft = emptyMap()
        countName = ""
        rebuildCurrentItems()
        persistDraftJob?.cancel()
        store.saveDraft(draft)
    }

    fun saveCount(): Boolean {
        val items = currentItems
        if (items.isEmpty()) return false
        history = listOf(HistoryEntry(newId(), System.currentTimeMillis(), countName.trim(), symbol, items)) + history
        store.saveHistory(history)
        clearCount()
        return true
    }

    fun deleteEntry(id: String) {
        history = history.filterNot { it.id == id }
        store.saveHistory(history)
    }

    fun clearHistory() {
        history = emptyList()
        store.saveHistory(history)
    }

    fun loadEntry(entry: HistoryEntry) {
        val list = denoms.toMutableList()
        val newDraft = mutableMapOf<String, Int>()
        for (item in entry.items) {
            val idx = list.indexOfFirst { it.cents == item.cents && it.type == item.type }
            val d = if (idx >= 0) {
                list[idx].copy(visible = true).also { list[idx] = it }
            } else {
                Denomination(newId(), item.cents, item.type).also { list += it }
            }
            newDraft[d.id] = item.qty
        }
        denoms = list
        draft = newDraft
        countName = entry.name
        rebuildDenomCaches()
        rebuildCurrentItems()
        persistDraftJob?.cancel()
        store.saveDenoms(denoms)
        store.saveDraft(draft)
    }

    fun setVisible(id: String, visible: Boolean) {
        denoms = denoms.map { if (it.id == id) it.copy(visible = visible) else it }
        rebuildDenomCaches()
        rebuildCurrentItems()
        store.saveDenoms(denoms)
    }

    fun addDenom(cents: Long, type: DenomType): Boolean {
        if (denoms.any { it.cents == cents && it.type == type }) return false
        denoms = denoms + Denomination(newId(), cents, type)
        rebuildDenomCaches()
        rebuildCurrentItems()
        store.saveDenoms(denoms)
        return true
    }

    fun deleteDenom(id: String) {
        denoms = denoms.filterNot { it.id == id }
        draft = draft - id
        rebuildDenomCaches()
        rebuildCurrentItems()
        persistDraftJob?.cancel()
        store.saveDenoms(denoms)
        store.saveDraft(draft)
    }

    fun resetDenoms() {
        denoms = defaultDenominations()
        draft = emptyMap()
        rebuildDenomCaches()
        rebuildCurrentItems()
        persistDraftJob?.cancel()
        store.saveDenoms(denoms)
        store.saveDraft(draft)
    }

    fun updateSymbol(value: String) {
        symbol = value.trim().take(4)
        store.saveSymbol(symbol)
    }

    override fun onCleared() {
        persistDraftJob?.cancel()
        store.saveDraft(draft)
        super.onCleared()
    }
}
