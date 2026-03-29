package com.example.quocardbookapi

import com.example.quocardbookapi.jooq.tables.references.AUTHORS
import com.example.quocardbookapi.jooq.tables.references.BOOKS
import com.example.quocardbookapi.jooq.tables.references.BOOK_AUTHORS
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AuthorRepository(
    private val dsl: DSLContext,
) {
    fun create(name: String, birthDate: LocalDate): Author =
        dsl.insertInto(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .returning()
            .fetchOne(::mapAuthorRecord)
            ?: error("Failed to create author")

    fun update(authorId: Long, name: String, birthDate: LocalDate): Author {
        val updated = dsl.update(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .where(AUTHORS.ID.eq(authorId))
            .returning()
            .fetchOne(::mapAuthorRecord)

        return updated ?: throw NotFoundException("Author $authorId was not found")
    }

    fun findById(authorId: Long): Author? =
        dsl.selectFrom(AUTHORS)
            .where(AUTHORS.ID.eq(authorId))
            .fetchOne(::mapAuthorRecord)

    fun countByIds(authorIds: List<Long>): Int =
        dsl.selectCount()
            .from(AUTHORS)
            .where(AUTHORS.ID.`in`(authorIds))
            .fetchSingle(0, Int::class.java) ?: 0

    private fun mapAuthorRecord(record: Record): Author = Author(
        id = record.get(AUTHORS.ID)!!,
        name = record.get(AUTHORS.NAME)!!,
        birthDate = record.get(AUTHORS.BIRTH_DATE)!!,
    )
}

@Repository
class BookRepository(
    private val dsl: DSLContext,
) {
    fun create(
        title: String,
        price: Int,
        publicationStatus: PublicationStatus,
        authorIds: List<Long>,
    ): Book {
        val bookId = dsl.insertInto(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
            .returning(BOOKS.ID)
            .fetchOne(BOOKS.ID)
            ?: error("Failed to create book")

        replaceAuthors(bookId, authorIds)
        return requireNotNull(findById(bookId))
    }

    fun update(
        bookId: Long,
        title: String,
        price: Int,
        publicationStatus: PublicationStatus,
        authorIds: List<Long>,
    ): Book {
        val updatedRows = dsl.update(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
            .where(BOOKS.ID.eq(bookId))
            .execute()

        if (updatedRows == 0) {
            throw NotFoundException("Book $bookId was not found")
        }

        replaceAuthors(bookId, authorIds)
        return requireNotNull(findById(bookId))
    }

    fun findById(bookId: Long): Book? =
        dsl.select(
            BOOKS.ID,
            BOOKS.TITLE,
            BOOKS.PRICE,
            BOOKS.PUBLICATION_STATUS,
            AUTHORS.ID,
            AUTHORS.NAME,
            AUTHORS.BIRTH_DATE,
        )
            .from(BOOKS)
            .join(BOOK_AUTHORS).on(BOOK_AUTHORS.BOOK_ID.eq(BOOKS.ID))
            .join(AUTHORS).on(AUTHORS.ID.eq(BOOK_AUTHORS.AUTHOR_ID))
            .where(BOOKS.ID.eq(bookId))
            .fetchGroups(BOOKS.ID)
            .entries
            .firstOrNull()
            ?.value
            ?.toBook()

    fun findByAuthorId(authorId: Long): List<Book> =
        dsl.select(
            BOOKS.ID,
            BOOKS.TITLE,
            BOOKS.PRICE,
            BOOKS.PUBLICATION_STATUS,
            AUTHORS.ID,
            AUTHORS.NAME,
            AUTHORS.BIRTH_DATE,
        )
            .from(BOOKS)
            .join(BOOK_AUTHORS).on(BOOK_AUTHORS.BOOK_ID.eq(BOOKS.ID))
            .join(AUTHORS).on(AUTHORS.ID.eq(BOOK_AUTHORS.AUTHOR_ID))
            .where(
                BOOKS.ID.`in`(
                    DSL.select(BOOK_AUTHORS.BOOK_ID)
                        .from(BOOK_AUTHORS)
                        .where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
                )
            )
            .orderBy(BOOKS.ID.asc(), AUTHORS.ID.asc())
            .fetchGroups(BOOKS.ID)
            .values
            .map { records -> records.toBook() }

    private fun replaceAuthors(bookId: Long, authorIds: List<Long>) {
        dsl.deleteFrom(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .execute()

        dsl.batch(
            authorIds.map { authorId ->
                dsl.insertInto(BOOK_AUTHORS)
                    .set(BOOK_AUTHORS.BOOK_ID, bookId)
                    .set(BOOK_AUTHORS.AUTHOR_ID, authorId)
            }
        ).execute()
    }

    private fun List<Record>.toBook(): Book {
        val first = first()
        return Book(
            id = first.get(BOOKS.ID)!!,
            title = first.get(BOOKS.TITLE)!!,
            price = first.get(BOOKS.PRICE)!!,
            publicationStatus = PublicationStatus.valueOf(first.get(BOOKS.PUBLICATION_STATUS)!!),
            authors = map { record ->
                Author(
                    id = record.get(AUTHORS.ID)!!,
                    name = record.get(AUTHORS.NAME)!!,
                    birthDate = record.get(AUTHORS.BIRTH_DATE)!!,
                )
            },
        )
    }
}
