package com.example.personal_secretary

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.notes.NoteModel
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate

// -------------------- DATA CLASSES --------------------
data class NoteRequest(
    val date: String,
    val user: String = "guest",
    val title: String,
    val description: String
)

// -------------------- RETROFIT API --------------------
interface ApiService {
    @GET("notes")
    suspend fun getNotes(): List<NoteModel>

    @POST("notes")
    suspend fun createNote(@Body note: NoteRequest): Response<NoteModel>
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:4000/"
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}


class NotesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotesScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    var notes by remember { mutableStateOf(listOf<NoteModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Fetch notes from backend
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            notes = ApiClient.apiService.getNotes()
            Log.d("NotesFetch", "Received notes: $notes")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            Text(text = "Notes", modifier = Modifier.padding(bottom = 12.dp))

            when {
                isLoading -> Text("Loading notes...")
                notes.isEmpty() -> Text("No notes found")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(notes) { note -> NoteItem(note) }
                }
            }
        }

        if (showForm) {
            AddNoteDialog(
                onDismiss = { showForm = false },
                onSave = { newNote ->
                    scope.launch {
                        try {
                            val noteToSend = newNote.copy(user = "guest")
                            val response = ApiClient.apiService.createNote(noteToSend)
                            if (response.isSuccessful) {
                                // Refresh notes after adding
                                notes = ApiClient.apiService.getNotes()
                                Log.d("NotesSave", "Note saved successfully")
                            } else {
                                Log.e("NotesSave", "Failed to save note: ${response.code()}")
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
    }
}


@Composable
fun NoteItem(note: NoteModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = note.date)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.description)
        }
    }
}


@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onSave: (NoteRequest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.height(150.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && description.isNotBlank()) {
                    val currentDate = LocalDate.now().toString()
                    onSave(
                        NoteRequest(
                            title = title,
                            description = description,
                            date = currentDate
                        )
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
