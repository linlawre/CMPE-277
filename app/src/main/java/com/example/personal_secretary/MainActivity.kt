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
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF64B5F6),
                            Color(0xFF1976D2)
                        )
                    )
                )
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Weather icon",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Today's Weather",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = weatherText,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHomeDaily() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF64B5F6),
                            Color(0xFF1976D2)
                        )
                    )
                )
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Default Today",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Daily Overview",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                        Text(
                            text = "Placeholder for AI Call summary",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                        )

                    Spacer(modifier = Modifier.height(8.dp))


                }
            }
        }
    }
}

@Composable
fun SummaryHomeWeekly() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF64B5F6),
                            Color(0xFF1976D2)
                        )
                    )
                )
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarViewWeek,
                    contentDescription = "Default Week",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Weekly Overview",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Placeholder for AI Call summary",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )

                }
            }
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
                        onClick = { selectedTab = 3
                                  context.startActivity(Intent(context,PlaidActivity::class.java))
                                  },
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
            SummaryHomeDaily()

            Spacer(modifier = Modifier.height(24.dp))


            //Temporarily leave this here so we can test the login activity, but eventually we should be starting at Login -> Go to main
            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }
            ) {
                Text("Open Login Page")
            }

            SummaryHomeWeekly()
        }
    }
}
