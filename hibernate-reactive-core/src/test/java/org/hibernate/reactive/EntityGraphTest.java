/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.unit.TestContext;

import static javax.persistence.FetchType.LAZY;

public class EntityGraphTest extends BaseReactiveTest {

	Author gail;
	Book oliphant;

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.addAnnotatedClass( Book.class );
		configuration.addAnnotatedClass( Author.class );
		return configuration;
	}

	@After
	public void cleanDb(TestContext context) {
		test( context, deleteEntities( Book.class, Author.class ) );
	}

	@Before
	public void populate(TestContext context) {
		gail = new Author( 5, " Gail Honeyman" );
		oliphant = new Book( "9780735220683", "Eleanor Oliphant Is Completely Fine ", gail );
		gail.books.add( oliphant );
		test( context, getMutinySessionFactory().withTransaction( session -> session
				.persistAll( gail, oliphant ) ) );
	}

	@Test
	public void testHqlPlanWithMutiny(TestContext context) {
		test( context, getMutinySessionFactory()
				.withSession( session -> session
						.createNamedQuery( Author.QUERY_NAME, Author.class )
						.setPlan( session.getEntityGraph( Author.class, Author.GRAPH_NAME ) )
						.getResultList() )
				.invoke( list -> {
					context.assertFalse( list.isEmpty() );
					final Author author = list.get( 0 );
					context.assertTrue( Hibernate.isInitialized( author.getBooks() ) );
					context.assertEquals( gail, author );
					context.assertEquals( oliphant, author.getBooks().get( 0 ) );
				} ) );
	}

	@Test
	public void testCriteriaPlanWithMutiny(TestContext context) {
		test( context, getMutinySessionFactory()
				.withSession( this::findAuthorsWithCriteria )
				.invoke( list -> {
					context.assertFalse( list.isEmpty() );
					final Author author = list.get( 0 );
					context.assertTrue( Hibernate.isInitialized( author.getBooks() ) );
					context.assertEquals( gail, author );
					context.assertEquals( oliphant, author.getBooks().get( 0 ) );
				} ) );
	}

	public Uni<List<Author>> findAuthorsWithCriteria(Mutiny.Session mutinySession) {
		CriteriaQuery<Author> query = getMutinySessionFactory().getCriteriaBuilder()
				.createQuery( Author.class );
		final Root<Author> root = query.from( Author.class );
		root.fetch( "books" );
		Mutiny.Query<Author> createQuery = mutinySession.createQuery( query );
		createQuery.setPlan( mutinySession.getEntityGraph( Author.class, Author.GRAPH_NAME ) );
		return createQuery.getResultList();
	}

	@Test
	public void testHqlPlanWithStage(TestContext context) {
		test( context, getMutinySessionFactory()
				.withSession( session -> session
						.createNamedQuery( Author.QUERY_NAME, Author.class )
						.setPlan( session.getEntityGraph( Author.class, Author.GRAPH_NAME ) )
						.getResultList() )
				.invoke( list -> {
					context.assertFalse( list.isEmpty() );
					final Author author = list.get( 0 );
					context.assertTrue( Hibernate.isInitialized( author.getBooks() ) );
					context.assertEquals( gail, author );
					context.assertEquals( oliphant, author.getBooks().get( 0 ) );
				} ) );
	}

	@Test
	public void testCriteriaPlanWithStage(TestContext context) {
		test( context, getSessionFactory()
				.withSession( this::findAuthorsWithCriteria )
				.thenAccept( list -> {
					context.assertFalse( list.isEmpty() );
					final Author author = list.get( 0 );
					context.assertTrue( Hibernate.isInitialized( author.getBooks() ) );
					context.assertEquals( gail, author );
					context.assertEquals( oliphant, author.getBooks().get( 0 ) );
				} ) );
	}

	public CompletionStage<List<Author>> findAuthorsWithCriteria(Stage.Session stageSession) {
		CriteriaQuery<Author> query = getSessionFactory().getCriteriaBuilder()
				.createQuery( Author.class );
		query.from( Author.class );
		Stage.Query<Author> createQuery = stageSession.createQuery( query );
		createQuery.setPlan( stageSession.getEntityGraph( Author.class, Author.GRAPH_NAME ) );
		return createQuery.getResultList();
	}

	@Entity
	@Table(name = "AUTHOR")
	@NamedEntityGraph(name = Author.GRAPH_NAME, attributeNodes = @NamedAttributeNode("books"))
	@NamedQuery(name = Author.QUERY_NAME, query = "FROM EntityGraphTest$Author")
	static class Author {

		static final String GRAPH_NAME = "graph.Author.books";
		static final String QUERY_NAME = "Author.listAll";

		@Id
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "author", cascade = CascadeType.PERSIST)
		private List<Book> books = new ArrayList<>();

		public Author() {
		}

		public Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Book> getBooks() {
			return books;
		}

		public void setBooks(List<Book> books) {
			this.books = books;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Author ) ) {
				return false;
			}
			Author author = (Author) o;
			return Objects.equals( name, author.name );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( name );
		}

		@Override
		public String toString() {
			return id + ":" + name;
		}
	}

	@Entity
	@Table(name = "BOOK")
	static class Book {
		@Id
		@GeneratedValue
		Integer id;

		String isbn;

		String title;

		@ManyToOne(fetch = LAZY)
		Author author;

		Book(String isbn, String title, Author author) {
			this.title = title;
			this.isbn = isbn;
			this.author = author;
		}

		Book() {
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Book ) ) {
				return false;
			}
			Book book = (Book) o;
			return Objects.equals( isbn, book.isbn ) && Objects.equals( title, book.title );
		}

		@Override
		public int hashCode() {
			return Objects.hash( isbn, title );
		}

		@Override
		public String toString() {
			return id + ":" + isbn + ":" + title;
		}
	}
}
