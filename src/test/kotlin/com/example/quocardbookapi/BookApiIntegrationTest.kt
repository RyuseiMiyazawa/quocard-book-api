package com.example.quocardbookapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `creates a book with multiple authors and fetches books by author`() {
        val author1 = createAuthor("Author One", LocalDate.of(1990, 1, 1))
        val author2 = createAuthor("Author Two", LocalDate.of(1992, 2, 2))

        mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateBookRequest(
                            title = "Domain-Driven Kotlin",
                            price = 3200,
                            publicationStatus = PublicationStatus.UNPUBLISHED,
                            authorIds = listOf(author1.id, author2.id),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Domain-Driven Kotlin"))
            .andExpect(jsonPath("$.authors.length()").value(2))

        mockMvc.perform(get("/authors/${author1.id}/books"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Domain-Driven Kotlin"))
            .andExpect(jsonPath("$[0].authors.length()").value(2))
    }

    @Test
    fun `rejects rollback from published to unpublished`() {
        val author = createAuthor("Author Three", LocalDate.of(1988, 3, 3))
        val book = createBook(author.id, PublicationStatus.PUBLISHED)

        mockMvc.perform(
            put("/books/${book.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        UpdateBookRequest(
                            title = book.title,
                            price = book.price,
                            publicationStatus = PublicationStatus.UNPUBLISHED,
                            authorIds = book.authors.map(AuthorResponse::id),
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Published books cannot be changed back to unpublished"))
    }

    @Test
    fun `rejects future birth date`() {
        mockMvc.perform(
            post("/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateAuthorRequest(
                            name = "Future Author",
                            birthDate = LocalDate.now().plusDays(1),
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    private fun createAuthor(name: String, birthDate: LocalDate): AuthorResponse {
        val response = mockMvc.perform(
            post("/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateAuthorRequest(name, birthDate)))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readValue(response, AuthorResponse::class.java)
    }

    private fun createBook(authorId: Long, status: PublicationStatus): BookResponse {
        val response = mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateBookRequest(
                            title = "Published Book",
                            price = 1000,
                            publicationStatus = status,
                            authorIds = listOf(authorId),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readValue(response, BookResponse::class.java)
    }
}
