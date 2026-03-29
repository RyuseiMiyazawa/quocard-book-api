package com.example.quocardbookapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PostgresBookApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `runs the main flow against postgresql`() {
        val author1 = createAuthor("Martin Fowler", LocalDate.of(1963, 12, 18))
        val author2 = createAuthor("Kent Beck", LocalDate.of(1961, 3, 31))

        mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreateBookRequest(
                            title = "Refactoring",
                            price = 4500,
                            publicationStatus = PublicationStatus.PUBLISHED,
                            authorIds = listOf(author1.id, author2.id),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
            .andExpect(jsonPath("$.authors.length()").value(2))

        mockMvc.perform(get("/authors/${author1.id}/books"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Refactoring"))
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

    companion object {
        @Container
        private val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("quocard_book_api_test")
            .withUsername("quocard")
            .withPassword("quocard")

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jooq.sql-dialect") { "POSTGRES" }
        }
    }
}
