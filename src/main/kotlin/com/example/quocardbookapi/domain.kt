package com.example.quocardbookapi

import java.time.LocalDate

enum class PublicationStatus {
    UNPUBLISHED,
    PUBLISHED
}

data class Author(
    val id: Long,
    val name: String,
    val birthDate: LocalDate,
)

data class Book(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authors: List<Author>,
)
