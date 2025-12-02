package com.example.personal_secretary


import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.time.LocalDate

data class TaskModel(
    val _id: String,
    val date: String,
    val description: String,
    val location: String? = null,
    val done: Boolean = false,
    val user:String="guest"
)

data class TaskRequest(
    val description: String,
    val location: String? = null,
    val date: String = LocalDate.now().toString(),
    val done: Boolean = false,
    val user: String ="guest"
)

object TempSpeechBuffer {
    var text: String = ""
}
interface TaskApiService {
    @GET("tasks")
    suspend fun getTasks(): List<TaskModel>

    @POST("tasks")
    suspend fun createTask(@Body task: TaskRequest): Response<TaskModel>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body task: TaskRequest): Response<TaskModel>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>
}

object TaskApiClient {
    private const val BASE_URL = "http://10.0.2.2:4000/"
    val apiService: TaskApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApiService::class.java)
    }
}


class TasksActivity : ComponentActivity() {

    private lateinit var email: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        email = intent.getStringExtra("EMAIL").toString()
        Log.d("NotesActivity", "Current email: $email")
        setContent {
            val theme = ThemeList.currentTheme
            Box(modifier = Modifier.fillMaxSize()) {
                if (theme.backgroundRes != 0) {
                    Image(
                        painter = painterResource(id = theme.backgroundRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    TasksScreen(email = email, onBack = { finish() })
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TasksScreen(
        email: String,
        modifier: Modifier = Modifier,
        onBack: () -> Unit
    ) {
        var tasks by remember { mutableStateOf(listOf<TaskModel>()) }
        var isLoading by remember { mutableStateOf(true) }
        var showForm by remember { mutableStateOf(false) }
        var selectedTask by remember { mutableStateOf<TaskModel?>(null) }

        val scope = rememberCoroutineScope()

        val userId = email
        val repo = ResponseRepository(LocalContext.current)

        LaunchedEffect(Unit) {
            isLoading = true
            try {
                tasks = TaskApiClient.apiService.getTasks().filter { it.user == email }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Tasks") },
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },

            floatingActionButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(onClick = { showForm = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                    SpeechToTextButton { spokenText ->
                        showForm = true
                        selectedTask = null
                        TempSpeechBuffer.text = spokenText
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(paddingValues)
            ) {

                when {
                    isLoading -> Text("Loading tasks...")
                    tasks.isEmpty() -> Text("No tasks found")
                    else -> {

                        val groupedTasks = tasks
                            .sortedBy { it.date }
                            .groupBy { it.date }

                        val flatList = mutableListOf<Any>()
                        groupedTasks.forEach { (date, tasksForDate) ->
                            flatList.add(date)
                            flatList.addAll(tasksForDate)
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(flatList) { item ->
                                when (item) {
                                    is String -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Color(0xFF1976D2),
                                                    shape = MaterialTheme.shapes.small
                                                )
                                                .padding(vertical = 6.dp, horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.titleMedium.copy(color=Color.White),
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }

                                    is TaskModel -> {
                                        TaskItem(
                                            task = item,
                                            onToggleDone = { toggledTask ->
                                                scope.launch {
                                                    val updated =
                                                        toggledTask.copy(done = !toggledTask.done)
                                                    try {
                                                        val response =
                                                            TaskApiClient.apiService.updateTask(
                                                                toggledTask._id,
                                                                TaskRequest(
                                                                    description = updated.description,
                                                                    location = updated.location,
                                                                    date = updated.date,
                                                                    done = updated.done,
                                                                    user = toggledTask.user
                                                                )
                                                            )
                                                        if (response.isSuccessful) {
                                                            tasks =
                                                                tasks.map { if (it._id == updated._id) updated else it }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            },
                                            onClick = { selectedTask = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showForm) {
                AddTaskDialog(
                    email = email,
                    onDismiss = { showForm = false },
                    onSave = { newTask ->
                        tasks = listOf(newTask.toTaskModel()) + tasks
                        scope.launch {
                            try {
                                TaskApiClient.apiService.createTask(newTask)
                                repo.clearResponse(userId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                showForm = false
                            }
                        }
                    }
                )
            }

            selectedTask?.let { task ->
                EditTaskDialog(
                    email = email,
                    task = task,
                    onDismiss = { selectedTask = null },
                    onSave = { updatedTask ->
                        tasks =
                            tasks.map { if (it._id == task._id) updatedTask.toTaskModel(task._id) else it }
                        scope.launch {
                            try {
                                TaskApiClient.apiService.updateTask(task._id, updatedTask)
                                repo.clearResponse(userId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                selectedTask = null
                            }
                        }
                    },
                    onDelete = { taskToDelete ->
                        tasks = tasks.filter { it._id != taskToDelete._id }
                        scope.launch {
                            try {
                                TaskApiClient.apiService.deleteTask(taskToDelete._id)
                                repo.clearResponse(userId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                selectedTask = null
                            }
                        }
                    }
                )
            }
        }
    }


    fun TaskRequest.toTaskModel(id: String = java.util.UUID.randomUUID().toString()): TaskModel =
        TaskModel(
            _id = id,
            date = this.date,
            description = this.description,
            location = this.location,
            done = this.done,
            user = this.user
        )

    @Composable
    fun TaskItem(task: TaskModel, onToggleDone: (TaskModel) -> Unit, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(8.dp)
                .background(Color.White.copy(alpha=0.8f), shape = MaterialTheme.shapes.medium),
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggleDone(task) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.description,
                style = if (task.done) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                else MaterialTheme.typography.bodyMedium
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddTaskDialog(
        email: String,
        onDismiss: () -> Unit,
        onSave: (TaskRequest) -> Unit
    ) {
        var description by remember { mutableStateOf(TempSpeechBuffer.text) }
        var location by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(LocalDate.now()) }
        var showDatePicker by remember { mutableStateOf(false) }


        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 24 * 60 * 60 * 1000
        )
        LaunchedEffect(Unit) {
            if (TempSpeechBuffer.text.isNotEmpty()) {
                description = TempSpeechBuffer.text
                TempSpeechBuffer.text = ""
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Optional)") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = date.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        modifier = Modifier.clickable { showDatePicker = true }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (description.isNotBlank()) {
                        onSave(
                            TaskRequest(
                                description = description,
                                location = location.ifBlank { null },
                                date = date.toString(),
                                user = email
                            )
                        )
                    }
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
        )
    }
    @Composable
    fun SpeechToTextButton(onResult: (String) -> Unit) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var isListening by remember { mutableStateOf(false) }

        val recognizer = remember {
            android.speech.SpeechRecognizer.createSpeechRecognizer(context)
        }

        val recognizerIntent = remember {
            android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            }
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                recognizer.startListening(recognizerIntent)
                isListening = true
            } else {
                onResult("Microphone permission denied")
            }
        }

        DisposableEffect(Unit) {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    isListening = false
                    val spoken = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    onResult(spoken)
                }
                override fun onError(error: Int) { isListening = false }
                override fun onReadyForSpeech(params: Bundle) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rms: Float) {}
                override fun onBufferReceived(buffer: ByteArray) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle) {}
                override fun onEvent(eventType: Int, params: Bundle) {}
            })

            onDispose { recognizer.destroy() }
        }

        FloatingActionButton(
            onClick = {
                launcher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        ) {
            if (isListening)
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            else
                Icon(Icons.Default.Mic, "Tap for Speech to Text")
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditTaskDialog(
        task: TaskModel,
        email: String,
        onDismiss: () -> Unit,
        onSave: (TaskRequest) -> Unit,
        onDelete: (TaskModel) -> Unit
    ) {
        var description by remember { mutableStateOf(task.description) }
        var location by remember { mutableStateOf(task.location ?: "") }
        var date by remember { mutableStateOf(LocalDate.parse(task.date)) }
        var showDatePicker by remember { mutableStateOf(false) }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 24 * 60 * 60 * 1000
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Optional)") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = date.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        modifier = Modifier.clickable { showDatePicker = true }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (description.isNotBlank()) {
                        onSave(
                            TaskRequest(
                                description = description,
                                location = location.ifBlank { null },
                                date = date.toString(),
                                user = email
                            )
                        )
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    Button(onClick = { onDelete(task) }) { Text("Delete") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) { Text("Cancel") }
                }
            }
        )
    }
}


