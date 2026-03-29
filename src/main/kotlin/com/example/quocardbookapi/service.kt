package com.example.quocardbookapi

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AuthorService(
    private val authorRepository: AuthorRepository,
    private val bookRepository: BookRepository,
) {
    @Transactional
    fun create(request: CreateAuthorRequest): Author =
        authorRepository.create(name = request.name.trim(), birthDate = request.birthDate)

    @Transactional
    fun update(authorId: Long, request: UpdateAuthorRequest): Author {
        ensureBirthDateIsNotFuture(request.birthDate)
        return authorRepository.update(authorId, request.name.trim(), request.birthDate)
    }

    @Transactional(readOnly = true)
    fun findBooks(authorId: Long): List<Book> {
        authorRepository.findById(authorId) ?: throw NotFoundException("Author $authorId was not found")
        return bookRepository.findByAuthorId(authorId)
    }

    private fun ensureBirthDateIsNotFuture(birthDate: LocalDate) {
        if (birthDate.isAfter(LocalDate.now())) {
            throw ValidationException("birthDate must be today or earlier")
        }
    }
}

@Service
class BookService(
    private val authorRepository: AuthorRepository,
    private val bookRepository: BookRepository,
) {
    @Transactional
    fun create(request: CreateBookRequest): Book {
        validateAuthorIds(request.authorIds)
        return bookRepository.create(
            title = request.title.trim(),
            price = request.price,
            publicationStatus = request.publicationStatus,
            authorIds = request.authorIds.distinct(),
        )
    }

    @Transactional
    fun update(bookId: Long, request: UpdateBookRequest): Book {
        validateAuthorIds(request.authorIds)
        val current = bookRepository.findById(bookId) ?: throw NotFoundException("Book $bookId was not found")
        if (current.publicationStatus == PublicationStatus.PUBLISHED &&
            request.publicationStatus == PublicationStatus.UNPUBLISHED
        ) {
            throw ValidationException("Published books cannot be changed back to unpublished")
        }
        return bookRepository.update(
            bookId = bookId,
            title = request.title.trim(),
            price = request.price,
            publicationStatus = request.publicationStatus,
            authorIds = request.authorIds.distinct(),
        )
    }

    private fun validateAuthorIds(authorIds: List<Long>) {
        if (authorIds.isEmpty()) {
            throw ValidationException("At least one author is required")
        }
        val distinctIds = authorIds.distinct()
        val foundCount = authorRepository.countByIds(distinctIds)
        if (foundCount != distinctIds.size) {
            throw ValidationException("All authorIds must reference existing authors")
        }
    }
}
