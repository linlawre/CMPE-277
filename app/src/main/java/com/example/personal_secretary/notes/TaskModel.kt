package com.example.personal_secretary.notes

data class TaskModel(
    val _id: String,
    val date: String,
    val description: String,
    val location: String? = null,
    val done: Boolean = false
)

