package com.example.quocardbookapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class BookServiceTest {
    private val authorRepository: AuthorRepository = mock()
    private val bookRepository: BookRepository = mock()
    private val bookService = BookService(authorRepository, bookRepository)

    @Test
    fun `rejects update from published to unpublished`() {
        whenever(bookRepository.findById(10L)).thenReturn(
            Book(
                id = 10L,
                title = "Published Book",
                price = 1000,
                publicationStatus = PublicationStatus.PUBLISHED,
                authors = listOf(Author(1L, "Author", LocalDate.of(1990, 1, 1))),
            )
        )
        whenever(authorRepository.countByIds(listOf(1L))).thenReturn(1)

        val ex = assertThrows(ValidationException::class.java) {
            bookService.update(
                10L,
                UpdateBookRequest(
                    title = "Published Book",
                    price = 1000,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorIds = listOf(1L),
                )
            )
        }

        assertEquals("Published books cannot be changed back to unpublished", ex.message)
        verify(bookRepository, never()).update(any(), any(), any(), any(), any())
    }

    @Test
    fun `rejects unknown author ids on create`() {
        whenever(authorRepository.countByIds(listOf(1L, 2L))).thenReturn(1)

        val ex = assertThrows(ValidationException::class.java) {
            bookService.create(
                CreateBookRequest(
                    title = "New Book",
                    price = 2000,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorIds = listOf(1L, 2L),
                )
            )
        }

        assertEquals("All authorIds must reference existing authors", ex.message)
        verify(bookRepository, never()).create(any(), any(), any(), any())
    }
}
