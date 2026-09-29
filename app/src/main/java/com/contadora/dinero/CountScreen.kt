package com.contadora.dinero

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CountScreen(vm: MoneyViewModel) {
    val context = LocalContext.current
    val denoms = vm.visibleDenoms
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (denoms.isEmpty()) {
            item { EmptyText("No hay denominaciones visibles. Actívalas en la pestaña Denominaciones.") }
        }
        items(denoms, key = { it.id }) { d ->
            DenomRow(d, vm.draft[d.id] ?: 0, vm.symbol) { vm.setQty(d.id, it) }
        }
        item {
            WhiteCard(Modifier.padding(top = 8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vm.countName,
                        onValueChange = { vm.countName = it.take(60) },
                        label = { Text("Nombre o nota (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { if (vm.draft.isNotEmpty() || vm.countName.isNotEmpty()) confirmClear = true },
                            modifier = Modifier.weight(1f),
                        ) { Text("Limpiar") }
                        Button(
                            onClick = {
                                val msg = if (vm.saveCount()) "Cuenta guardada" else "Ingresa al menos una cantidad"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Guardar") }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            message = "¿Limpiar la cuenta actual?",
            confirmText = "Limpiar",
            onConfirm = vm::clearCount,
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun DenomRow(d: Denomination, qty: Int, symbol: String, onQty: (Int) -> Unit) {
    val color = d.type.color()
    WhiteCard {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(color, size = Size(5.dp.toPx(), size.height)) }
                .padding(start = 17.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        d.type.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        Money.denom(d.cents, symbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                FilledTonalIconButton(onClick = { onQty((qty - 1).coerceAtLeast(0)) }) {
                    Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = if (qty == 0) "" else qty.toString(),
                    onValueChange = { text -> onQty(text.filter(Char::isDigit).take(7).toIntOrNull() ?: 0) },
                    modifier = Modifier.width(88.dp),
                    singleLine = true,
                    placeholder = { Text("0", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                )
                FilledTonalIconButton(onClick = { onQty(qty + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar")
                }
            }
            Text(
                "Subtotal: " + Money.format(d.cents * qty, symbol),
                Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
                color = MutedColor,
            )
        }
    }
}
