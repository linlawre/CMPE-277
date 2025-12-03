/**
 * This is the homescreen of our application coming from Login Screen.
 * User will be greeted with three cards
 *      Weather
 *      Daily
 *      Weekly
 *
 * And also a navigation bar at the bottom to navigate to other screens
 */



package com.example.personal_secretary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MainActivity : ComponentActivity() {

    private var email: String = "Unknown"
    private lateinit var requestPermissionLauncher:
            androidx.activity.result.ActivityResultLauncher<String>

    /**
     * on accessing the page for the first time
     * Load theme (if saved in RoomDB)
     * And take the email sent from LoginActivity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = intent.getStringExtra("EMAIL") ?: "Unknown"
        // Microphone permission launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) println("🎤 Microphone access granted")
                else println("❌ Microphone access denied")
            }

        enableEdgeToEdge()


        //Load Theme
        ThemeList.loadTheme(this, email) {
            runOnUiThread {
                setContent {
                    HomeScreen(
                        checkMicrophonePermission = { checkMicrophonePermission() },
                        email = email
                    )
                }
            }
        }
    }

    /**
     * This is a testing function for permissions. This is not used here
     */
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

/**
 * Sets up the WeatherCard using functions from WeatherActivity
 * It should prompt the user for GPS and ping OpenWeatherMap API for day-summaries
 */
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

    //On page open, get GPS
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

    //Create the card
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
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        fontWeight= FontWeight.Bold
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

/**
 * Sets up the Daily card
 * This will retrieve tasks from MongoDB and check for todays date. If so, it sends it to OpenAI for OpenAI to provide a suggestion
 * If response is received, save in RoomDB, otherwise tell user to enjoy their day
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SummaryHomeDaily(email: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("Loading daily summary...") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(email) {
        isLoading = true
        error = null
        try {

            val tasks = withContext(Dispatchers.IO) {
                try {
                    TaskApiClient.apiService.getTasks()
                } catch (e: Exception) {
                    emptyList<TaskModel>()
                }
            }.filter { it.user == email && !it.done }

            val today = LocalDate.now().toString()
            val todays = tasks.filter { it.date == today }

            if (todays.isEmpty()) {
                text = "No pending tasks! Enjoy!"
            } else {

                //Prompt building, while also trying to reduce the amount of tokens and making it seem less AI based by making it more concise
                val prompt = buildString {
                    append("Please summarize the following tasks for today ($today) for the user.\n")
                    append("Provide a short concise summary and suggested next steps.\n\n")
                    append("Note that the user will not respond to you in any way, information you give should be more action-based and not request more information as none will be given")
                    todays.forEachIndexed { idx, t ->
                        append("${idx + 1}. ${t.description}")
                        if (!t.location.isNullOrBlank()) append(" — ${t.location}")
                        append("\n")
                    }
                }

                val repo = ResponseRepository(context)
                val userId = email

                val saved = repo.getSavedResponse(userId)
                if (saved != null) {
                    text = saved.response
                } else {
                    text = "Generating summary..."
                    val response = sendToBackend(context, prompt)
                    text = response
                    repo.saveResponse(userId,LocalDate.now().toString(), response)
                }

            }
        } catch (e: Exception) {
            error = "Error generating daily summary: ${e.localizedMessage ?: "unknown"}"
        } finally {
            isLoading = false
        }
    }
    //Create the card
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
                            Color(0xFF81C784),
                            Color(0xFF388E3C)
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
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Generating summary...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                            )
                        }
                    } else {
                        error?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Yellow)
                            )
                        } ?: Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )

                    }
                }
            }
        }
    }
}

/**
 * Sets up the Weekly Card
 * This will retrieve tasks from MongoDB and check for Monday-Sunday date. If so, it sends it to OpenAI for OpenAI to provide a suggestion
 * If response is received, save in RoomDB, otherwise tell user to enjoy their day
 */
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun SummaryHomeWeekly(email: String) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var isLoading by remember { mutableStateOf(true) }
        var text by remember { mutableStateOf("Loading weekly summary...") }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(email) {
            isLoading = true
            error = null
            try {
                val tasks = withContext(Dispatchers.IO) {
                    try {
                        TaskApiClient.apiService.getTasks()
                    } catch (e: Exception) {
                        emptyList<TaskModel>()
                    }
                }.filter { it.user == email && !it.done }

                val today = LocalDate.now()
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

                val weekTasks = tasks.filter {
                    try {
                        val d = LocalDate.parse(it.date)
                        !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
                    } catch (e: Exception) {
                        false
                    }
                }

                if (weekTasks.isEmpty()) {
                    text = "No pending tasks! Enjoy!"
                } else {

                    val prompt = buildString {
                        append("Please summarize the user's tasks for the current week (${startOfWeek} to ${endOfWeek}).\n")
                        append("Give a concise weekly summary and suggested priorities.\n\n")
                        append("Note that the user will not respond to you in any way, information you give should be more action-based and not request more information as none will be given")
                        weekTasks.forEachIndexed { idx, t ->
                            append("${idx + 1}. ${t.date} — ${t.description}")
                            if (!t.location.isNullOrBlank()) append(" — ${t.location}")
                            append("\n")
                        }
                    }
                    val repo = ResponseRepository(context)
                    val userId = email

                    val saved = repo.getSavedResponse(userId)
                    if (saved != null) {
                        text = saved.response
                    } else {
                        text = "Generating quick summary..."
                        val response = sendToBackend(context, prompt)
                        text = response
                        repo.saveResponse(userId, LocalDate.now().toString(), response)
                    }
                }
            } catch (e: Exception) {
                error = "Error generating weekly summary: ${e.localizedMessage ?: "unknown"}"
            } finally {
                isLoading = false
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
                                Color(0xFFBA68C8),
                                Color(0xFF7B1FA2)
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
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                            fontWeight = FontWeight.Bold
                        )
                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "Generating weekly summary...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                                )
                            }
                        } else {
                            error?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Yellow)
                                )
                            } ?: Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Create the composable of the Screen which will contain the Cards we created above (Weather/Daily/Weekly)
 */
    @Composable
    fun HomeScreen(
        checkMicrophonePermission: () -> Unit,
        email: String
    ) {
        var selectedTab by remember { mutableIntStateOf(0) }
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val scrollState = rememberScrollState()



        DisposableEffect(Unit) {


            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    selectedTab = 0
                }
            }

            lifecycle.addObserver(observer)

            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

    //Set Theme
        Box(modifier = Modifier.fillMaxSize()) {
            if (ThemeList.currentTheme.backgroundRes != 0) {
                Image(
                    painter = painterResource(id = ThemeList.currentTheme.backgroundRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,



                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    context.startActivity(
                                        Intent(context, NotesActivity::class.java).apply {
                                            putExtra("EMAIL", email)
                                        }
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.StickyNote2,
                                        contentDescription = "Notes"
                                    )
                                },
                                label = { Text("Notes") }
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                    context.startActivity(
                                        Intent(context, TasksActivity::class.java).apply {
                                            putExtra("EMAIL", email)
                                        }
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = "Tasks"
                                    )
                                },
                                label = { Text("Tasks") }
                            )

                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = {
                                    selectedTab = 3
                                    context.startActivity(
                                        Intent(
                                            context,
                                            PlaidActivity::class.java
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = "Budget"
                                    )
                                },
                                label = { Text("Budget") }
                            )

                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = {
                                    selectedTab = 4
                                    context.startActivity(
                                        Intent(context, SettingActivity::class.java).apply {
                                            putExtra("EMAIL", email)
                                        }
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") }
                            )
                        }
                    }


                ) { innerPadding ->


                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),

                        verticalArrangement = Arrangement.Top
                    ) {
                        Box (
                            modifier = Modifier.fillMaxWidth()
                                .background((Color(0xAA000000)))
                                .padding(8.dp)
                        ) {
                            Text(
                                "Welcome to your Personal Secretary",
                                fontSize = 18.sp,
                                maxLines = 1,
                                color=Color.White,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                        // I believe we should do a summarized view here, most likely will add my OpenAI calls here too.

                        Spacer(modifier = Modifier.height(24.dp))
                        WeatherCardHome()

                        //This one we might not keep on this page, maybe throw it into the notes/task section
                      //  Button(onClick = checkMicrophonePermission) {
                       //     Text("Enable Microphone")
                       // }
                        SummaryHomeDaily(email)
                        SummaryHomeWeekly(email)
                        Spacer(modifier = Modifier.height(24.dp))


                        //Temporarily leave this here so we can test the login activity, but eventually we should be starting at Login -> Go to main
                      //  Button(
                      //      onClick = {
                      //          context.startActivity(Intent(context, LoginActivity::class.java))
                      //      }
                      //  ) {
                      //      Text("Open Login Page")
                      //  }


                    }
                }
            }
        }

