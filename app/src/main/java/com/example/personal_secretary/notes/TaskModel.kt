package com.example.personal_secretary.notes

data class TaskModel(
    val id: Int,
    val subjectLine:String,
    val date: String,          //Tasks need a date; to be determined
    val location: String? = null, // Tasks need a location
    val description: String,
    val isImportant: Boolean
)
