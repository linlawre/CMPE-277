package com.example.personal_secretary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

class SettingActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("EMAIL") ?: "Unknown"

        setContent {
            val context = LocalContext.current
            var showThemeDialog by remember { mutableStateOf(false) }
            var currentTheme by remember { mutableStateOf(ThemeList.currentTheme) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = { showThemeDialog = true }) {
                        Text("Change Theme")
                    }

                    if (showThemeDialog) {
                        AlertDialog(
                            onDismissRequest = { showThemeDialog = false },
                            title = { Text("Select Theme") },
                            text = {
                                Column {
                                    ThemeList.Theme.values().forEach { theme ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable {
                                                    ThemeList.saveTheme(context, email, theme) {
                                                        currentTheme = theme
                                                    }
                                                    showThemeDialog = false
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(theme.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showThemeDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
}
