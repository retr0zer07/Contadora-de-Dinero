package com.contadora.dinero

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(color, size = Size(4.dp.toPx(), size.height)) }
                .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Money.denom(d.cents, symbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    Money.format(d.cents * qty, symbol),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedColor,
                )
            }
            FilledTonalIconButton(
                onClick = { onQty((qty - 1).coerceAtLeast(0)) },
                modifier = Modifier.size(36.dp),
            ) {
                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            BasicTextField(
                value = if (qty == 0) "" else qty.toString(),
                onValueChange = { text -> onQty(text.filter(Char::isDigit).take(7).toIntOrNull() ?: 0) },
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(68.dp)
                    .height(38.dp)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(Teal),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (qty == 0) Text("0", color = MutedColor, fontSize = 16.sp)
                        inner()
                    }
                },
            )
            FilledTonalIconButton(
                onClick = { onQty(qty + 1) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Sumar", Modifier.size(20.dp))
            }
        }
    }
}
