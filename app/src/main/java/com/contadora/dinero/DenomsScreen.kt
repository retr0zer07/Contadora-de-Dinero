@file:OptIn(ExperimentalMaterial3Api::class)

package com.contadora.dinero

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DenomsScreen(vm: MoneyViewModel) {
    val context = LocalContext.current
    var valueText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(DenomType.BILL) }
    var toDelete by remember { mutableStateOf<Denomination?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Moneda")
                    OutlinedTextField(
                        value = vm.symbol,
                        onValueChange = vm::updateSymbol,
                        label = { Text("Símbolo") },
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                }
            }
        }

        item {
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Nueva denominación")
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { t -> valueText = t.filter { it.isDigit() || it == '.' || it == ',' }.take(15) },
                        label = { Text("Valor (ej. 20000)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DenomType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                        }
                    }
                    Button(
                        onClick = {
                            val cents = Money.parseCents(valueText)
                            val msg = when {
                                cents == null -> "Valor inválido"
                                !vm.addDenom(cents, type) -> "Esa denominación ya existe"
                                else -> {
                                    valueText = ""
                                    "Agregada: ${Money.denom(cents, vm.symbol)}"
                                }
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Agregar") }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("Visibles al contar")
                }
                TextButton(onClick = { confirmReset = true }) { Text("Restaurar") }
            }
        }

        item {
            WhiteCard {
                val list = vm.sortedDenoms
                if (list.isEmpty()) EmptyText("Sin denominaciones")
                list.forEachIndexed { i, d ->
                    if (i > 0) HorizontalDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.setVisible(d.id, !d.visible) }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = d.visible, onCheckedChange = { vm.setVisible(d.id, it) })
                        Text(
                            d.type.label,
                            Modifier.width(68.dp),
                            color = d.type.color(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            Money.denom(d.cents, vm.symbol),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = { toDelete = d }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MutedColor)
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { d ->
        ConfirmDialog(
            message = "¿Eliminar la denominación ${Money.denom(d.cents, vm.symbol)} (${d.type.label.lowercase()})?",
            confirmText = "Eliminar",
            onConfirm = { vm.deleteDenom(d.id) },
            onDismiss = { toDelete = null },
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            message = "¿Restaurar las denominaciones predeterminadas? Se eliminarán las personalizadas y la cuenta actual.",
            confirmText = "Restaurar",
            onConfirm = vm::resetDenoms,
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
