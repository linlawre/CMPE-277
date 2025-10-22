package com.example.personal_secretary


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme


class TasksActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TasksScreen()
                }
            }
        }
    }
}



@Composable
fun TasksScreen(modifier: Modifier = Modifier){
 //   val tasks = TasksData.tasks

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)
        .statusBarsPadding())
    {
        Text(
            text="Tasks",
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}