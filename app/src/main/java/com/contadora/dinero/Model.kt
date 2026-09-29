package com.contadora.dinero

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.UUID

enum class DenomType(val label: String) { BILL("Billete"), COIN("Moneda") }

// Los valores se guardan en centavos para evitar errores de redondeo
data class Denomination(
    val id: String,
    val cents: Long,
    val type: DenomType,
    val visible: Boolean = true,
)

data class CountItem(val cents: Long, val type: DenomType, val qty: Int) {
    val subtotal: Long get() = cents * qty
}

data class HistoryEntry(
    val id: String,
    val date: Long,
    val name: String,
    val symbol: String,
    val items: List<CountItem>,
) {
    val total: Long get() = items.sumOf { it.subtotal }
    val pieces: Int get() = items.sumOf { it.qty }
}

fun newId(): String = UUID.randomUUID().toString()

fun defaultDenominations(): List<Denomination> =
    listOf<Long>(1000, 500, 200, 100, 50, 20).map { Denomination(newId(), it * 100, DenomType.BILL) } +
        listOf<Long>(1000, 500, 200, 100, 50).map { Denomination(newId(), it, DenomType.COIN) }

object Money {
    private val decimal = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val integer = NumberFormat.getIntegerInstance()

    fun format(cents: Long, symbol: String): String = symbol + decimal.format(cents / 100.0)

    fun denom(cents: Long, symbol: String): String =
        symbol + if (cents % 100 == 0L) integer.format(cents / 100) else decimal.format(cents / 100.0)

    fun count(n: Int): String = integer.format(n)

    fun parseCents(text: String): Long? = runCatching {
        text.trim().replace(',', '.').toBigDecimal()
            .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()?.takeIf { it in 1..999_999_999_999_99L }
}
