package com.example.personal_secretary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.*

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher:
            androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Microphone permission launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) println("🎤 Microphone access granted")
                else println("❌ Microphone access denied")
            }

        enableEdgeToEdge()

        setContent {
            Personal_SecretaryTheme {
                HomeScreen(checkMicrophonePermission = { checkMicrophonePermission() })
            }
        }
    }

    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                println("🎤 Microphone access already granted")
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
@Composable
fun WeatherCardHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var weatherText by remember { mutableStateOf("Loading weather...") }
    var isLoading by remember { mutableStateOf(true) }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scope.launch {
                    fetchWeather(context) { result ->
                        weatherText = result
                        isLoading = false
                    }
                }
            } else {
                weatherText = "Location permission denied"
                isLoading = false
            }
        }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                fetchWeather(context) { result ->
                    weatherText = result
                    isLoading = false
                }
            }
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Today's Weather",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Text(
                    text = weatherText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SummaryHome() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Placeholder for my summary home WIP",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

        }
    }
}


@Composable
fun HomeScreen(checkMicrophonePermission: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),


        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.20f)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    val context = LocalContext.current

                    @Composable
                    fun NavButton(
                        selected: Boolean,
                        onClick: () -> Unit,
                        icon: ImageVector,
                        label: String
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .padding(vertical = 8.dp)
                                .clickable { onClick() },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    NavButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = Icons.Default.Home,
                        label = "Home"
                    )

                    NavButton(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            context.startActivity(Intent(context, NotesActivity::class.java))
                        },
                        icon = Icons.Default.StickyNote2,
                        label = "Notes"
                    )

                    NavButton(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            context.startActivity(Intent(context, TasksActivity::class.java))
                        },
                        icon = Icons.Default.Checklist,
                        label = "Tasks"
                    )

                    //This button wont work yet, I still need to put in my plaid API call
                    NavButton(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = Icons.Default.AttachMoney,
                        label = "Budget"
                    )

                    //Settings I think we should basically just use the previous homework assignment and just do dark/light.
                    // Maybe provide an option to opt into my AI features
                    NavButton(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = Icons.Default.Settings,
                        label = "Settings"
                    )
                }
            }
        }

    ) { innerPadding ->


        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Welcome to your Personal Secretary",
                style = MaterialTheme.typography.headlineMedium
            )
            // I believe we should do a summarized view here, most likely will add my OpenAI calls here too.

            Spacer(modifier = Modifier.height(24.dp))
            WeatherCardHome()

            //This one we might not keep on this page, maybe throw it into the notes/task section
            Button(onClick = checkMicrophonePermission) {
                Text("Enable Microphone")
            }
            SummaryHome()

            Spacer(modifier = Modifier.height(24.dp))


            //Temporarily leave this here so we can test the login activity, but eventually we should be starting at Login -> Go to main
            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }
            ) {
                Text("Open Login Page")
            }
        }
    }
}
