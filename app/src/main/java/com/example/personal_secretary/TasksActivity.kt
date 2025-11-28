package com.example.personal_secretary

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate


data class TaskModel(
    val _id: String,
    val date: String,
    val description: String,
    val location: String? = null,
    val done: Boolean = false
)

data class TaskRequest(
    val description: String,
    val location: String? = null,
    val date: String = LocalDate.now().toString(),
    val done: Boolean = false
)


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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TasksScreen(onBack = { finish() })
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var tasks by remember { mutableStateOf(listOf<TaskModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskModel?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            tasks = TaskApiClient.apiService.getTasks()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
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
            FloatingActionButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            Text(text = "Tasks", modifier = Modifier.padding(bottom = 12.dp))

            when {
                isLoading -> Text("Loading tasks...")
                tasks.isEmpty() -> Text("No tasks found")
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortedTasks = tasks.sortedBy { it.done }
                    items(sortedTasks) { task ->
                        TaskItem(task = task, onToggleDone = { toggledTask ->
                            scope.launch {
                                val updated = toggledTask.copy(done = !toggledTask.done)
                                try {
                                    val response = TaskApiClient.apiService.updateTask(toggledTask._id, TaskRequest(
                                        description = updated.description,
                                        location = updated.location,
                                        done = updated.done
                                    ))
                                    if (response.isSuccessful) {
                                        tasks = tasks.map { if (it._id == updated._id) updated else it }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }, onClick = { selectedTask = task })
                    }
                }
            }
        }

        if (showForm) {
            AddTaskDialog(
                onDismiss = { showForm = false },
                onSave = { newTask ->
                    scope.launch {
                        try {
                            val response = TaskApiClient.apiService.createTask(newTask)
                            if (response.isSuccessful) {
                                tasks = TaskApiClient.apiService.getTasks()
                            }
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
                task = task,
                onDismiss = { selectedTask = null },
                onSave = { updatedTask ->
                    scope.launch {
                        try {
                            val response = TaskApiClient.apiService.updateTask(task._id, updatedTask)
                            if (response.isSuccessful) {
                                tasks = TaskApiClient.apiService.getTasks()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            selectedTask = null
                        }
                    }
                },
                onDelete = { taskToDelete ->
                    scope.launch {
                        try {
                            val response = TaskApiClient.apiService.deleteTask(taskToDelete._id)
                            if (response.isSuccessful) {
                                tasks = TaskApiClient.apiService.getTasks()
                            }
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

@Composable
fun TaskItem(task: TaskModel, onToggleDone: (TaskModel) -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
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

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onSave: (TaskRequest) -> Unit) {
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

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
            }
        },
        confirmButton = {
            Button(onClick = {
                if (description.isNotBlank()) {
                    onSave(TaskRequest(description = description, location = location.ifBlank { null }))
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditTaskDialog(
    task: TaskModel,
    onDismiss: () -> Unit,
    onSave: (TaskRequest) -> Unit,
    onDelete: (TaskModel) -> Unit
) {
    var description by remember { mutableStateOf(task.description) }
    var location by remember { mutableStateOf(task.location ?: "") }

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
            }
        },
        confirmButton = {
            Button(onClick = {
                if (description.isNotBlank()) {
                    onSave(TaskRequest(description = description, location = location.ifBlank { null }))
                }
            }) {
                Text("Save")
            }
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
