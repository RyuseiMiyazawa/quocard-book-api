package com.example.quocardbookapi

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PastOrPresent
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

data class CreateAuthorRequest(
    @field:NotBlank
    val name: String,
    @field:PastOrPresent
    val birthDate: LocalDate,
)

data class UpdateAuthorRequest(
    @field:NotBlank
    val name: String,
    @field:PastOrPresent
    val birthDate: LocalDate,
)

data class CreateBookRequest(
    @field:NotBlank
    val title: String,
    @field:Min(0)
    val price: Int,
    val publicationStatus: PublicationStatus,
    @field:NotEmpty
    val authorIds: List<Long>,
)

data class UpdateBookRequest(
    @field:NotBlank
    val title: String,
    @field:Min(0)
    val price: Int,
    val publicationStatus: PublicationStatus,
    @field:NotEmpty
    val authorIds: List<Long>,
)

data class AuthorResponse(
    val id: Long,
    val name: String,
    val birthDate: LocalDate,
)

data class BookResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authors: List<AuthorResponse>,
)

@RestController
@RequestMapping("/authors")
class AuthorController(
    private val authorService: AuthorService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateAuthorRequest): AuthorResponse =
        authorService.create(request).toResponse()

    @PutMapping("/{authorId}")
    fun update(
        @PathVariable authorId: Long,
        @Valid @RequestBody request: UpdateAuthorRequest,
    ): AuthorResponse = authorService.update(authorId, request).toResponse()

    @GetMapping("/{authorId}/books")
    fun findBooks(@PathVariable authorId: Long): List<BookResponse> =
        authorService.findBooks(authorId).map(Book::toResponse)
}

@RestController
@RequestMapping("/books")
class BookController(
    private val bookService: BookService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateBookRequest): BookResponse =
        bookService.create(request).toResponse()

    @PutMapping("/{bookId}")
    fun update(
        @PathVariable bookId: Long,
        @Valid @RequestBody request: UpdateBookRequest,
    ): BookResponse = bookService.update(bookId, request).toResponse()
}

private fun Author.toResponse(): AuthorResponse = AuthorResponse(
    id = id,
    name = name,
    birthDate = birthDate,
)

private fun Book.toResponse(): BookResponse = BookResponse(
    id = id,
    title = title,
    price = price,
    publicationStatus = publicationStatus,
    authors = authors.map(Author::toResponse),
)
