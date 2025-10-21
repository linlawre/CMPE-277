package com.example.personal_secretary.notes

/**
 * Sample data to use temporarily as test until link to DB
 * Can be used as a fallback if DB is unaccessible
 */
object NotesData {
    val notes = listOf(
        NoteModel(
            id = 1,
            date = "2025-10-20",
            location = "Library - SJSU",
            description = "Met with Lawrence to discuss this app. Assign tasks and deadlines."
        ),
        NoteModel(
            id = 2,
            date = "2025-10-18",
            location = null,
            description = "Phone numbers here: 4081231121."
        ),
        NoteModel(
            id = 3,
            date = "2025-10-10",
            location = "Cafe 88",
            description = "Random User/PW "
        ),
        NoteModel(
            id = 4,
            date = "2025-09-30",
            location = "Home",
            description = "SJSU best website"
        )
    )
}