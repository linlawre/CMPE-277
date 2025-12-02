package com.example.personal_secretary

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.notes.NoteModel
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.LocalDate
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

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
                Surface(modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent) {
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
    email: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var notes by remember { mutableStateOf(listOf<NoteModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteModel?>(null) }
    var selectedTemplateDescription by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        isLoading = true
        try {
            notes = ApiClient.apiService.getNotes().filter { it.user == email }
        } catch (e: Exception) { e.printStackTrace() }
        finally { isLoading = false }
    }

    val templates = mapOf(
        "Grocery List" to """
            • Milk
            • Eggs
            • Bread
            • Vegetables
            • Fruits
            • Meat
        """.trimIndent(),
        "To-Do Checklist" to """
            • Morning Routine
            • Work Tasks
            • Exercise
            • Study/Reading
            • Evening Tasks
        """.trimIndent(),
        "Meeting Notes" to """
            • Attendees:
            • Agenda:
            • Key Points:
            • Action Items:
            • Next Meeting:
        """.trimIndent()
    )

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
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> Text("Loading notes...")
                notes.isEmpty() -> Text("No notes found")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(notes) { note ->
                        NoteItem(note = note) { selectedNote = note }
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Choose a Template",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    templates.forEach { (name, description) ->
                        Text(
                            text = name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTemplateDescription = description
                                    showBottomSheet = false
                                    showAddDialog = true
                                }
                                .padding(vertical = 8.dp)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))


                    Text(
                        text = "Blank Note",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTemplateDescription = ""
                                showBottomSheet = false
                                showAddDialog = true
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }


        if (showAddDialog) {
            AddNoteDialog(
                email = email,
                initialDescription = selectedTemplateDescription,
                onDismiss = { showAddDialog = false },
                onSave = { newNote ->
                    scope.launch {
                        try {
                            val response = ApiClient.apiService.createNote(newNote)
                            if (response.isSuccessful) {
                                notes = ApiClient.apiService.getNotes().filter { it.user == email }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                        finally { showAddDialog = false }
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
                                notes = ApiClient.apiService.getNotes().filter { it.user == email }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                        finally { selectedNote = null }
                    }
                },
                onDelete = { noteToDelete ->
                    scope.launch {
                        try {
                            val response = ApiClient.apiService.deleteNote(noteToDelete._id)
                            if (response.isSuccessful) {
                                notes = ApiClient.apiService.getNotes().filter { it.user == email }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                        finally { selectedNote = null }
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
        .clickable { onClick()},
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha=0.8f))
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
    email: String,
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onSave: (NoteRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(initialDescription) }

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
            }) { Text("Save") }
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

