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
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Personal_SecretaryTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Error state
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text("Email") },
            isError = emailError != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (emailError != null) Text(emailError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (passwordError != null) Text(passwordError!!, color = MaterialTheme.colorScheme.error)

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            var hasError = false

            if (!email.contains("@")) {
                emailError = "Invalid email"
                hasError = true
            }

            if (password.length < 6) {
                passwordError = "Password must be at least 6 characters"
                hasError = true
            }

            if (hasError) return@Button

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = NetworkClient.login(email, password)
                    CoroutineScope(Dispatchers.Main).launch {
                        if (response.success) {
                            val intent = Intent(context, MainActivity::class.java)
                            intent.putExtra("EMAIL", email)
                            context.startActivity(intent)
                            (context as? ComponentActivity)?.finish()
                        } else {
                            Toast.makeText(context, response.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }

        Spacer(Modifier.height(12.dp))

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
