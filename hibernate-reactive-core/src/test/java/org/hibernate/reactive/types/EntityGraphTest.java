/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.graph.Graph;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;

@Timeout(value = 10, timeUnit = MINUTES)
public class EntityGraphTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Author.class, Book.class, Chapter.class );
	}

	private static final String FIND_AUTHOR_BY_NAME_LIKE_QUERY = "FROM Author a WHERE a.name LIKE :name ORDER BY a.name";

	// Adding the join fetch will make everything work
	//	private static final String FIND_AUTHOR_BY_NAME_LIKE_QUERY = "FROM Author a left join fetch a.books b left join fetch b.chapters WHERE a.name LIKE :name ORDER BY a.name";

	private SessionFactory ormFactory;

	@BeforeEach
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( DRIVER, dbType().getJdbcDriver() );
		configuration.setProperty( DIALECT, dbType().getDialectClass().getName() );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@AfterEach
	public void closeOrmFactory() {
		ormFactory.close();
	}

	public List<Author> createAuthors() {
		Author author1 = new Author();
		author1.setName("Author1");

		Book book1 = new Book();
		book1.setTitle("Book 1");
		book1.setPages(50);
		author1.addBook( book1 );

		Chapter chapter11 = new Chapter();
		chapter11.setName("Book1 - Ch1");
		book1.addChapters( chapter11 );

		Chapter chapter12 = new Chapter();
		chapter12.setName("Book1 - Ch2");
		book1.addChapters( chapter12 );

		Book book2 = new Book();
		book2.setTitle("Book 2");
		book2.setPages(20);
		author1.addBook( book2 );

		Chapter chapter21 = new Chapter();
		chapter21.setName("Book1 - Ch1");
		book2.addChapters( chapter21 );

		Author author2 = new Author();
		author2.setName("Author2");
		return List.of( author2, author1 );
	}

	@Test
	public void testWithOrm() {
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			for ( Author author : createAuthors() ) {
				session.persist( author );
			}
			session.getTransaction().commit();
		}
		Author author = null;
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			author = session
					.createSelectionQuery( FIND_AUTHOR_BY_NAME_LIKE_QUERY, Author.class )
					.setParameter( "name", "Author1" )
					.setMaxResults( 1 )
					.setEntityGraph( createORMEntityGraph( session ), GraphSemantic.LOAD )
					.getSingleResult();
			assertThat( author.getBooks() ).hasSize( 2 );
			session.getTransaction().commit();
		}

	}

	private EntityGraph<Author> createORMEntityGraph(Session session) {
		RootGraph<Author> rootGraph = session.createEntityGraph( Author.class );
		rootGraph.addAttributeNode( "books" );
		Graph<?> graph = rootGraph.addSubGraph( "books" );
		graph.addAttributeNode( "chapters" );
		return rootGraph;
	}

	@Test
	public void test(VertxTestContext context) {
		test( context, getSessionFactory()
				.withTransaction( s -> s.persist( createAuthors().toArray() ) )
				.thenCompose( v -> findByNameLike( "Author%" ) )
				.thenAccept( author -> assertThat( author.getBooks() ).hasSize( 2 ) )
		);
	}

	public CompletionStage<Author> findByNameLike(String name) {
		return getSessionFactory().withTransaction( session -> session
				.createSelectionQuery( FIND_AUTHOR_BY_NAME_LIKE_QUERY, Author.class )
				.setParameter( "name", name )
				.setMaxResults( 1 )
				.setPlan( createEntityGraph( session ) )
				.getSingleResult()
		);
	}

	private EntityGraph<Author> createEntityGraph(Stage.Session session) {
		RootGraph<Author> rootGraph = (RootGraph<Author>) session.createEntityGraph( Author.class );
		rootGraph.addAttributeNode( "books" );
		Graph<?> graph = rootGraph.addSubGraph( "books" );
		graph.addAttributeNode( "chapters" );
		return rootGraph;
	}

	@Entity(name = "Author")
	public static class Author {

		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "author")
		Set<Book> books = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public void setBooks(Set<Book> books) {
			this.books = books;
		}

		public void addBook(Book book) {
			books.add( book );
			book.author = this;
		}
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		Long id;

		String title;

		int pages;

		@ManyToOne
		Author author;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "book")
		List<Chapter> chapters = new ArrayList<>();

		public void addChapters(Chapter... chapters) {
			for ( Chapter chapter : chapters ) {
				this.chapters.add( chapter );
				chapter.book = this;
			}
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public int getPages() {
			return pages;
		}

		public void setPages(int pages) {
			this.pages = pages;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}

		public List<Chapter> getChapters() {
			return chapters;
		}

		public void setChapters(List<Chapter> chapters) {
			this.chapters = chapters;
		}
	}

	@Entity(name = "Chapter")
	public static class Chapter {

		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		Book book;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}
	}
}
