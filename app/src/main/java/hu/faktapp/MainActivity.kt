@file:OptIn(ExperimentalMaterial3Api::class)

package hu.faktapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1565C0),
                    secondaryContainer = Color(0xFFD6E6F7)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    FaktApp()
                }
            }
        }
    }
}

@Composable
fun FaktApp() {
    val vm: MainViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = vm.tasks.firstOrNull { it.id == selectedId }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Feladatok") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("Kiadottak") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("Áttekintés") }
                )
            }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {
            when (tab) {
                0 -> FeladatokScreen(vm) { selectedId = it.id }
                1 -> KiadottakScreen(vm) { selectedId = it.id }
                else -> AttekintesScreen(vm)
            }
        }
    }

    if (selected != null) {
        TaskDialog(
            task = selected,
            mark = vm.marks[selected.id],
            onSave = { status, note ->
                vm.setMark(selected.id, status, note)
                selectedId = null
            },
            onDismiss = { selectedId = null }
        )
    }
}
