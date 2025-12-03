/**
 * Settings Page provides three options
 * SELECT Theme from Presets
 * Change password
 * Logout
 */
package com.example.personal_secretary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sets up the entire Settings page in OnCreate including buttons and theming
 */
class SettingActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("EMAIL") ?: "Unknown"

        val themeState = mutableStateOf(ThemeList.currentTheme)
        ThemeList.loadTheme(this, email) {
            themeState.value = ThemeList.currentTheme
        }
        setContent {
            val context = LocalContext.current
            var showThemeDialog by remember { mutableStateOf(false) }
            var showPasswordDialog by remember { mutableStateOf(false) }
            var oldPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var changePasswordMessage by remember { mutableStateOf<String?>(null) }
            Box(modifier = Modifier.fillMaxSize()) {

                if (themeState.value.backgroundRes != 0) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = themeState.value.backgroundRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

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
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Theme")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Password")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        finish()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Logout")
                    }

                    if (showThemeDialog) {
                        AlertDialog(
                            onDismissRequest = { showThemeDialog = false },
                            title = { Text("Select Theme") },
                            text = {
                                Column {
                                    ThemeList.Theme.entries.forEach { theme ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable {
                                                    ThemeList.saveTheme(context, email, theme) {
                                                        themeState.value = theme
                                                    }
                                                    showThemeDialog = false
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                theme.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showThemeDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    if (showPasswordDialog) {
                        AlertDialog(
                            onDismissRequest = { showPasswordDialog = false },
                            title = { Text("Change Password") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = oldPassword,
                                        onValueChange = { oldPassword = it },
                                        label = { Text("Old Password") },
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("New Password") },
                                        singleLine = true
                                    )
                                    changePasswordMessage?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    showPasswordDialog = false
                                    changePasswordMessage = null
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val response = NetworkClient.changePassword(
                                                email,
                                                oldPassword,
                                                newPassword
                                            )
                                            withContext(Dispatchers.Main) {
                                                changePasswordMessage = if (response.success) {
                                                    "Password changed successfully!"
                                                } else {
                                                    response.message ?: "Failed to change password"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                changePasswordMessage =
                                                    "Error: ${e.localizedMessage}"
                                            }
                                        }
                                    }
                                }) {
                                    Text("Submit")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showPasswordDialog = false }) {
                                    Text("Cancel")
                                }
                            })

                    }
                }
            }
                }
            }
        }}


