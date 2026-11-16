package com.partyrockers.appsonos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.partyrockers.appsonos.ui.screens.EqScreen
import com.partyrockers.appsonos.ui.screens.FoldersScreen
import com.partyrockers.appsonos.ui.screens.HomeScreen
import com.partyrockers.appsonos.ui.screens.LibraryScreen
import com.partyrockers.appsonos.ui.screens.PlaylistsScreen
import com.partyrockers.appsonos.ui.screens.ProfileScreen
import com.partyrockers.appsonos.ui.theme.AppSonosTheme
import com.partyrockers.appsonos.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSonosTheme {
                AppSonosApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    BIBLIOTECA("Biblioteca", Icons.Filled.LibraryMusic),
    CARPETAS("Carpetas", Icons.Filled.Folder),
    PLAYLISTS("Playlists", Icons.Filled.QueueMusic),
    EQ("EQ", Icons.Filled.Equalizer),
}

/**
 * App con 5 pestañas abajo (sin pestaña de Letras: la letra solo se ve
 * en Inicio, sobre la portada, y solo si la rola tiene letra).
 * El Perfil se abre con el avatar de arriba a la derecha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSonosApp(playerViewModel: PlayerViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(Tab.INICIO) }
    var showProfile by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Sonos") },
                actions = {
                    IconButton(onClick = { showProfile = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                for (tab in Tab.values()) {
                    NavigationBarItem(
                        selected = selectedTab == tab && !showProfile,
                        onClick = {
                            selectedTab = tab
                            showProfile = false
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (showProfile) {
                ProfileScreen(
                    viewModel = playerViewModel,
                    onClose = { showProfile = false },
                )
            } else {
                when (selectedTab) {
                    Tab.INICIO -> HomeScreen(viewModel = playerViewModel)
                    Tab.BIBLIOTECA -> LibraryScreen(viewModel = playerViewModel)
                    Tab.CARPETAS -> FoldersScreen(viewModel = playerViewModel)
                    Tab.PLAYLISTS -> PlaylistsScreen(viewModel = playerViewModel)
                    Tab.EQ -> EqScreen(viewModel = playerViewModel)
                }
            }
        }
    }
}
