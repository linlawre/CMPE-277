package com.example.personal_secretary.notes

data class NoteModel(
    val date: String,
    val user: String? = null,
    val title: String,
    val description: String,
    val _id: String?= null,
    val __v: Int?=null
)