package com.example.personal_secretary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.notes.NoteModel
import com.example.personal_secretary.notes.NotesData
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme

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

@Composable
fun NotesScreen(modifier: Modifier= Modifier) {
    val notes = NotesData.notes

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)
        .statusBarsPadding())
        {
        Text(
            text = "Notes",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (notes.isEmpty()) {
            Text("No notes found")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(notes) { note ->
                    NoteItem(note = note)
                }
            }
        }
    }
}
@Composable
fun NoteItem(note: NoteModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),

            ) {
                Text(
                    text = note.date,
                )

                note.location?.let { loc ->
                    Text(
                        text = loc,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.description
            )
        }
    }
}