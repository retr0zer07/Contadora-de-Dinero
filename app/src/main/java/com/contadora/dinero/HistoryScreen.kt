package com.contadora.dinero

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MoneyViewModel, onLoaded: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    var selected by remember { mutableStateOf<HistoryEntry?>(null) }
    var toDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Historial",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (vm.history.isNotEmpty()) {
                    TextButton(
                        onClick = { confirmClearAll = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Borrar todo") }
                }
            }
        }
        if (vm.history.isEmpty()) {
            item { EmptyText("Aún no hay cuentas guardadas.") }
        }
        items(vm.history, key = { it.id }) { h ->
            WhiteCard(onClick = { selected = h }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            h.name.ifBlank { "Cuenta sin nombre" },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${dateFormat.format(Date(h.date))} · ${Money.count(h.pieces)} piezas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        Money.format(h.total, h.symbol),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    selected?.let { h ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(h.name.ifBlank { "Cuenta sin nombre" }) },
            text = { HistoryDetail(h, dateFormat.format(Date(h.date))) },
            confirmButton = {
                TextButton(onClick = {
                    vm.loadEntry(h)
                    selected = null
                    Toast.makeText(context, "Cuenta cargada en el contador", Toast.LENGTH_SHORT).show()
                    onLoaded()
                }) { Text("Cargar") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { toDelete = h },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Eliminar") }
                    TextButton(onClick = { selected = null }) { Text("Cerrar") }
                }
            },
        )
    }

    toDelete?.let { h ->
        ConfirmDialog(
            message = "¿Eliminar esta cuenta del historial?",
            confirmText = "Eliminar",
            onConfirm = {
                vm.deleteEntry(h.id)
                selected = null
            },
            onDismiss = { toDelete = null },
        )
    }

    if (confirmClearAll) {
        ConfirmDialog(
            message = "¿Borrar todo el historial? Esta acción no se puede deshacer.",
            confirmText = "Borrar",
            onConfirm = vm::clearHistory,
            onDismiss = { confirmClearAll = false },
        )
    }
}

@Composable
private fun HistoryDetail(h: HistoryEntry, date: String) {
    Column {
        Text(date, style = MaterialTheme.typography.bodySmall, color = MutedColor)
        Spacer(Modifier.padding(4.dp))
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            DetailRow("Denom.", "Cant.", "Subtotal", header = true)
            h.items.forEach { i ->
                HorizontalDivider()
                DetailRow(
                    "${Money.denom(i.cents, h.symbol)} ${if (i.type == DenomType.BILL) "B" else "M"}",
                    Money.count(i.qty),
                    Money.format(i.subtotal, h.symbol),
                )
            }
        }
        HorizontalDivider(thickness = 2.dp)
        DetailRow("Total", Money.count(h.pieces), Money.format(h.total, h.symbol), header = true)
    }
}

@Composable
private fun DetailRow(a: String, b: String, c: String, header: Boolean = false) {
    val weight = if (header) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(a, Modifier.weight(1.3f), fontWeight = weight)
        Text(b, Modifier.weight(0.7f), fontWeight = weight, textAlign = TextAlign.End)
        Text(c, Modifier.weight(1.4f), fontWeight = weight, textAlign = TextAlign.End)
    }
}
