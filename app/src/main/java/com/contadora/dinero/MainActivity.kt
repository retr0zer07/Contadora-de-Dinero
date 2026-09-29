package com.contadora.dinero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Teal,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFCCFBF1),
                    onPrimaryContainer = Color(0xFF134E4A),
                    secondaryContainer = Color(0xFFCCFBF1),
                    onSecondaryContainer = Color(0xFF134E4A),
                    background = Color(0xFFF1F5F9),
                ),
            ) {
                ContadoraApp()
            }
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    COUNT("Contar", Icons.Default.Create),
    HISTORY("Historial", Icons.Default.DateRange),
    DENOMS("Denominaciones", Icons.Default.Settings),
}

@Composable
fun ContadoraApp(vm: MoneyViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.COUNT) }
    BackHandler(enabled = tab != Tab.COUNT) { tab = Tab.COUNT }

    Scaffold(
        topBar = { Header(vm) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label, maxLines = 1) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.COUNT -> CountScreen(vm)
                Tab.HISTORY -> HistoryScreen(vm, onLoaded = { tab = Tab.COUNT })
                Tab.DENOMS -> DenomsScreen(vm)
            }
        }
    }
}

@Composable
private fun Header(vm: MoneyViewModel) {
    val items = vm.currentItems
    val total = items.sumOf { it.subtotal }
    val bills = items.filter { it.type == DenomType.BILL }.sumOf { it.qty }
    val coins = items.filter { it.type == DenomType.COIN }.sumOf { it.qty }
    val soft = Color.White.copy(alpha = 0.85f)

    Surface(color = Teal, contentColor = Color.White, shadowElevation = 4.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("Contadora · Total", style = MaterialTheme.typography.labelLarge, color = soft)
            Text(
                Money.format(total, vm.symbol),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${Money.count(bills + coins)} piezas · ${Money.count(bills)} billetes · ${Money.count(coins)} monedas",
                style = MaterialTheme.typography.bodySmall,
                color = soft,
            )
        }
    }
}
