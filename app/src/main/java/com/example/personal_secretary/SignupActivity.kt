package com.example.personal_secretary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set Compose UI theme + screen
        setContent {
            Personal_SecretaryTheme {
                SignupScreen()
            }
        }
    }
}

@Composable
fun SignupScreen() {

    val context = LocalContext.current

    // User input fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Error messages for each input
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // EMAIL INPUT FIELD
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null // Reset error when user types
            },
            label = { Text("Email") },
            isError = emailError != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (emailError != null) Text(emailError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(16.dp))

        // PASSWORD INPUT FIELD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null // Reset error on change
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(), // Hide text
            isError = passwordError != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (passwordError != null) Text(passwordError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(16.dp))

        // CONFIRM PASSWORD INPUT
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmError = null // Reset error on change
            },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmError != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (confirmError != null) Text(confirmError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(24.dp))

        // SIGN UP BUTTON
        Button(
            onClick = {

                // ---- Basic Validation ----
                var hasError = false

                if (!email.contains("@")) {
                    emailError = "Invalid email"
                    hasError = true
                }

                if (password.length < 6) {
                    passwordError = "Password must be at least 6 characters"
                    hasError = true
                }

                if (password != confirmPassword) {
                    confirmError = "Passwords do not match"
                    hasError = true
                }

                // Stop if any errors exist
                if (hasError) return@Button

                // ---- NETWORK CALL IN BACKGROUND ----
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = NetworkClient.signup(email, password)

                        // Switch to main thread for UI updates
                        CoroutineScope(Dispatchers.Main).launch {
                            if (response.success) {

                                Toast.makeText(
                                    context,
                                    "Sign up successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Move to MainActivity
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)

                                // Close signup activity
                                (context as? ComponentActivity)?.finish()

                            } else {
                                // Backend error message
                                Toast.makeText(
                                    context,
                                    response.message ?: "Sign up failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } catch (e: Exception) {
                        // Handle failed network / server error
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
            Text("Sign Up")
        }

        Spacer(Modifier.height(12.dp))

        // Go to login screen
        TextButton(
            onClick = {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish()
            }
        ) {
            Text("Already have an account? Login")
        }
    }
}
