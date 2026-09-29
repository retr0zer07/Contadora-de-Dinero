package com.contadora.dinero

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("contadora", Context.MODE_PRIVATE)

    fun loadDenoms(): List<Denomination>? = read(KEY_DENOMS) { s ->
        JSONArray(s).objects().map { o ->
            Denomination(
                o.getString("id"),
                o.getLong("cents"),
                DenomType.valueOf(o.getString("type")),
                o.getBoolean("visible"),
            )
        }
    }

    fun saveDenoms(list: List<Denomination>) = write(KEY_DENOMS, JSONArray(list.map { d ->
        JSONObject()
            .put("id", d.id)
            .put("cents", d.cents)
            .put("type", d.type.name)
            .put("visible", d.visible)
    }).toString())

    fun loadHistory(): List<HistoryEntry> = read(KEY_HISTORY) { s ->
        JSONArray(s).objects().map { o ->
            HistoryEntry(
                o.getString("id"),
                o.getLong("date"),
                o.optString("name"),
                o.optString("symbol", "$"),
                o.getJSONArray("items").objects().map { i ->
                    CountItem(i.getLong("cents"), DenomType.valueOf(i.getString("type")), i.getInt("qty"))
                },
            )
        }
    } ?: emptyList()

    fun saveHistory(list: List<HistoryEntry>) = write(KEY_HISTORY, JSONArray(list.map { h ->
        JSONObject()
            .put("id", h.id)
            .put("date", h.date)
            .put("name", h.name)
            .put("symbol", h.symbol)
            .put("items", JSONArray(h.items.map { i ->
                JSONObject().put("cents", i.cents).put("type", i.type.name).put("qty", i.qty)
            }))
    }).toString())

    fun loadDraft(): Map<String, Int> = read(KEY_DRAFT) { s ->
        val o = JSONObject(s)
        o.keys().asSequence().associateWith { o.getInt(it) }
    } ?: emptyMap()

    fun saveDraft(draft: Map<String, Int>) = write(KEY_DRAFT, JSONObject(draft).toString())

    fun loadSymbol(): String = prefs.getString(KEY_SYMBOL, "$") ?: "$"

    fun saveSymbol(symbol: String) = write(KEY_SYMBOL, symbol)

    private fun <T> read(key: String, parse: (String) -> T): T? =
        prefs.getString(key, null)?.let { runCatching { parse(it) }.getOrNull() }

    private fun write(key: String, value: String) = prefs.edit().putString(key, value).apply()

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

    private companion object {
        const val KEY_DENOMS = "denoms"
        const val KEY_HISTORY = "history"
        const val KEY_DRAFT = "draft"
        const val KEY_SYMBOL = "symbol"
    }
}
