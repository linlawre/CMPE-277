package com.example.personal_secretary.notes

data class NoteModel(
    val id: Int,
    val date: String,          //Not sure if notes need a date; to be determined
    val location: String? = null, // My idea here is that maybe notes will need a location?
    val description: String
)