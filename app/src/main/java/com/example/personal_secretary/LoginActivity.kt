package com.example.personal_secretary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set Compose UI as layout
        setContent {
            Personal_SecretaryTheme {
                LoginScreen() // Render login page
            }
        }
    }
}

@Composable
fun LoginScreen() {

    val context = androidx.compose.ui.platform.LocalContext.current

    // Input states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Validation error message states
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TITLE
        Text(
            text = "Personal Secretary",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp // Larger title size
            )
        )

        Spacer(Modifier.height(54.dp))

        // LOGO
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))



        Text("Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        // EMAIL INPUT FIELD
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null   // Clear error when typing
            },
            label = { Text("Email") },
            isError = emailError != null,
            modifier = Modifier.fillMaxWidth()
        )

        // Email error text
        if (emailError != null)
            Text(emailError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(16.dp))

        // PASSWORD INPUT FIELD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null  // Clear error when typing
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null,
            modifier = Modifier.fillMaxWidth()
        )

        // Password error message
        if (passwordError != null)
            Text(passwordError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(24.dp))

        // LOGIN BUTTON
        Button(
            onClick = {

                var hasError = false

                // Simple email validation
                if (!email.contains("@")) {
                    emailError = "Invalid email"
                    hasError = true
                }

                // Password length validation
                if (password.length < 6) {
                    passwordError = "Password must be at least 6 characters"
                    hasError = true
                }

                // Stop login if validation failed
                if (hasError) return@Button

                // Launch network request in background thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = NetworkClient.login(email, password)

                        // Switch to main thread to update UI
                        CoroutineScope(Dispatchers.Main).launch {
                            if (response.success) {

                                // Navigate to MainActivity
                                val intent = Intent(context, MainActivity::class.java)
                                intent.putExtra("EMAIL", email)
                                context.startActivity(intent)

                                // Close login page
                                (context as? ComponentActivity)?.finish()

                            } else {
                                Toast.makeText(
                                    context,
                                    response.message ?: "Login failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {

                        // Show error if network fails
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                "Network error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(Modifier.height(12.dp))

        // Go to Sign Up
        TextButton(
            onClick = {
                val intent = Intent(context, SignupActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}
