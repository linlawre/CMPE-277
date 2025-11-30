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
import com.example.personal_secretary.notes.NoteModel
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate

data class NoteRequest(
    val date: String,
    val user: String = "guest",
    val title: String,
    val description: String
)


interface ApiService {
    @GET("notes")
    suspend fun getNotes(): List<NoteModel>

    @POST("notes")
    suspend fun createNote(@Body note: NoteRequest): Response<NoteModel>

    @PUT("notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body note: NoteRequest): Response<NoteModel>

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Map<String, Any>>
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
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        email = intent.getStringExtra("EMAIL").toString()
        Log.d("NotesActivity", "Current email: $email")
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotesScreen(
                        email = email,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    email:String,
    modifier: Modifier = Modifier,
     onBack: () -> Unit) {
    var notes by remember { mutableStateOf(listOf<NoteModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteModel?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            notes = ApiClient.apiService.getNotes().filter {it.user==email}
            Log.d("NotesFetch", "Received notes: $notes")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
                    items(notes) {
                        note -> NoteItem(note=note){
                            selectedNote= note
                    }
                    }
                }
            }
        }

        if (showForm) {
            AddNoteDialog(
                email=email,
                onDismiss = { showForm = false },
                onSave = { newNote ->
                    scope.launch {
                        try {
                            val response = ApiClient.apiService.createNote(newNote)
                            Log.d("NotesActivity", "Creating note w/ Email: $email")
                            if (response.isSuccessful) {

                                notes = ApiClient.apiService.getNotes().filter { it.user ==email}
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


        selectedNote?.let { note ->
            EditNoteDialog(
                note = note,
                onDismiss = { selectedNote = null },
                onSave = { updatedNote ->
                    scope.launch {
                        try {

                                val response = ApiClient.apiService.updateNote(note._id, updatedNote)
                                if (response.isSuccessful) {

                                    notes = ApiClient.apiService.getNotes().filter {it.user==email}
                                    Log.d("NotesEdit", "Note updated successfully")
                                } else {
                                    Log.e("NotesEdit", "Failed to update note: ${response.code()}")
                                }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            selectedNote = null
                        }
                    }
                },
                onDelete = { noteToDelete ->
                    scope.launch {
                        try {
                            val response = ApiClient.apiService.deleteNote(noteToDelete._id)
                            if (response.isSuccessful) {
                                notes = ApiClient.apiService.getNotes().filter {it.user==email}
                                Log.d("NotesDelete", "Note deleted successfully")
                            } else {
                                Log.e("NotesDelete", "Failed to delete note: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            selectedNote = null
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun NoteItem(note: NoteModel, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick()}
    ) {
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
fun AddNoteDialog(
    email:String,
    onDismiss: () -> Unit,
    onSave: (NoteRequest) -> Unit) {
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
                            date = currentDate,
                            user = email
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

@Composable
fun EditNoteDialog(
    note: NoteModel,
    onDismiss: () -> Unit,
    onSave: (NoteRequest) -> Unit,
    onDelete: (NoteModel) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var description by remember { mutableStateOf(note.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note") },
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
                    onSave(
                        NoteRequest(
                            title = title,
                            description = description,
                            date = note.date
                        )
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                Button(onClick = { onDelete(note) }) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

