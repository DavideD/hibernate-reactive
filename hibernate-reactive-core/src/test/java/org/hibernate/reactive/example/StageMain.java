/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.example;

import io.vertx.ext.unit.TestContext;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.hibernate.reactive.BaseReactiveTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static java.time.Month.*;

public class StageMain extends BaseReactiveTest {

    @Override
    protected Collection<Class<?>> annotatedEntities() {
        return List.of( Book.class, Author.class );
    }

    @Test
    public void testCriteriaEntityQuery(TestContext context) {
        Author author1 = new Author("Iain M. Banks");
        Author author2 = new Author("Neal Stephenson");
        Book book1 = new Book("1-85723-235-6", "Feersum Endjinn", author1, LocalDate.of(1994, JANUARY, 1));
        Book book2 = new Book("0-380-97346-4", "Cryptonomicon", author2, LocalDate.of(1999, MAY, 1));
        Book book3 = new Book("0-553-08853-X", "Snow Crash", author2, LocalDate.of(1992, JUNE, 1));
        author1.getBooks().add(book1);
        author2.getBooks().add(book2);
        author2.getBooks().add(book3);

        CriteriaBuilder builder = getSessionFactory().getCriteriaBuilder();
        CriteriaQuery<Book> query = builder.createQuery(Book.class);
        Root<Book> b = query.from(Book.class);
        b.fetch("author");
        query.orderBy(builder.asc(b.get("isbn")));

        CriteriaUpdate<Book> update = builder.createCriteriaUpdate(Book.class);
        b = update.from(Book.class);
        update.set(b.get("title"), "XXX");

        CriteriaDelete<Book> delete = builder.createCriteriaDelete(Book.class);
        b = delete.from(Book.class);

        /*
	public void reactiveWithTransactionStatelessSession(TestContext context) {
		final GuineaPig guineaPig = new GuineaPig( 61, "Mr. Peanutbutter" );
		test( context, getSessionFactory()
				.withStatelessTransaction( session -> session.insert( guineaPig ) )
				.thenCompose( v -> getSessionFactory()
						.withSession( session -> session.find( GuineaPig.class, guineaPig.getId() ) ) )
				.thenAccept( result -> assertThatPigsAreEqual( context, guineaPig, result ) )
		);
	}
         */
        test(
                context,
                getSessionFactory().withStatelessTransaction(
                        session -> session.insert( author1, author2, book1, book2, book3 )
//                                .thenCompose( v -> session.flush() )
                        )
//                        .thenCompose( v -> openSession() )
//                        .thenCompose( session -> session.createQuery( query ).getResultList() )
//                        .thenAccept( books -> {
//                            context.assertEquals( 3, books.size() );
//                            books.forEach( book -> {
//                                context.assertNotNull( book.id );
//                                context.assertNotNull( book.title );
//                                context.assertNotNull( book.isbn );
//                            } );
//                        } )
//                        .thenCompose( v -> openSession() )
//                        .thenCompose( session -> session.createQuery( update ).executeUpdate() )
//                        .thenCompose( v -> openSession() )
//                        .thenCompose( session -> session.createQuery( delete ).executeUpdate() )
        );
    }

    @Entity
    @Table(name="authors")
    public static class Author {
        @Id
        @GeneratedValue
        private Integer id;

        @NotNull
        private String name;

        @OneToMany(mappedBy = "author", cascade = CascadeType.PERSIST)
        private List<Book> books = new ArrayList<>();

        public Author(String name) {
            super();
            this.name = name;
        }

        public Author() {}

        Integer getId() {
            return id;
        }

        String getName() {
            return name;
        }

        List<Book> getBooks() {
            return books;
        }
    }


    @Entity
    @Table(name="books")
    public static class Book {
        @Id
        @GeneratedValue
        private Integer id;

        private String isbn;

        @NotNull
        private String title;

        @Basic(fetch = LAZY)
        @NotNull
        private LocalDate published;

        @Basic(fetch = LAZY)
        public byte[] coverImage;

        @NotNull
        @ManyToOne(fetch = LAZY)
        private Author author;

        public Book(String isbn, String title, Author author, LocalDate published) {
            super();
            this.title = title;
            this.isbn = isbn;
            this.author = author;
            this.published = published;
            this.coverImage = ("Cover image for '" + title + "'").getBytes();
        }

        public Book() {}

        Integer getId() {
            return id;
        }

        String getIsbn() {
            return isbn;
        }

        String getTitle() {
            return title;
        }

        Author getAuthor() {
            return author;
        }

        LocalDate getPublished() {
            return published;
        }

        byte[] getCoverImage() {
            return coverImage;
        }
    }
}
