/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;
import org.assertj.core.api.Assertions;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.MYSQL;

/**
 * This test class verifies that data can be persisted and queried on the same database
 * using both JPA/hibernate and reactive session factories.
 */
public class ORMReactivePersistenceTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( MYSQL );

	private SessionFactory ormFactory;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Author.class, SpellBook.class, Book.class );
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.SHOW_SQL, "true" );
		configuration.setProperty( Settings.DRIVER, "com.mysql.cj.jdbc.Driver" );
		configuration.setProperty( Settings.DIALECT, MySQL8Dialect.class.getName() );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@After
	public void cleanDb(TestContext context) {
		ormFactory.close();
	}

	/**
	 * Non reaactive version of {@link UnionSubclassInheritanceTest#testQueryUpdateWithParameters(TestContext)}
	 * @see UnionSubclassInheritanceTest#testQueryUpdateWithParameters(TestContext)
	 */
	@Test
	public void testORMWitMutinySession(TestContext context) {
		final SpellBook spells = new SpellBook( 6, "Necronomicon", true, new Date() );

		try (Session s = ormFactory.openSession()) {
			s.beginTransaction();
			s.persist( spells );
			s.getTransaction().commit();
		}

		try (Session s = ormFactory.openSession()) {
			s.beginTransaction();
			s.createQuery("update SpellBook set forbidden=:fob where title=:tit")
					.setParameter("fob", false)
					.setParameter("tit", "Necronomicon")
					.executeUpdate();
			s.getTransaction().commit();
		}

		try (Session s = ormFactory.openSession()) {
			s.beginTransaction();
			s.createQuery("update Book set title=title||:sfx where title=:tit")
					.setParameter("sfx", " II")
					.setParameter("tit", "Necronomicon")
					.executeUpdate();
			s.getTransaction().commit();
		}

		try (Session s = ormFactory.openSession()) {
			Book book = s.find( Book.class, 6 );
			Assertions.assertThat( book ).isNotNull();
		}

		try (Session s = ormFactory.openSession()) {
			s.beginTransaction();
			s.createQuery( "delete Book where title=:tit" )
					.setParameter( "tit", "Necronomicon II" )
					.executeUpdate();
			s.getTransaction().commit();
		}

		try (Session s = ormFactory.openSession()) {
			Book book = s.find( Book.class, 6 );
			Assertions.assertThat( book ).isNull();
		}
	}

	@Entity(name="SpellBook")
	@Table(name = "SpellBookUS")
	@DiscriminatorValue("S")
	public static class SpellBook extends Book {

		private boolean forbidden;

		public SpellBook(Integer id, String title, boolean forbidden, Date published) {
			super(id, title, published);
			this.forbidden = forbidden;
		}

		SpellBook() {}

		public boolean getForbidden() {
			return forbidden;
		}
	}

	@Entity(name="Book")
	@Table(name = "BookUS")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Book {

		@Id private Integer id;
		private String title;
		@Temporal(TemporalType.DATE)
		private Date published;

		public Book() {
		}

		public Book(Integer id, String title, Date published) {
			this.id = id;
			this.title = title;
			this.published = published;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Date getPublished() {
			return published;
		}

		public void setPublished(Date published) {
			this.published = published;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Book book = (Book) o;
			return Objects.equals( title, book.title );
		}

		@Override
		public int hashCode() {
			return Objects.hash( title );
		}
	}

	@Entity(name = "Author")
	@Table(name = "AuthorUS")
	public static class Author {

		@Id @GeneratedValue
		private Integer id;

		@Column(name = "`name`")
		private String name;

		@ManyToOne
		private Book book;

		public Author() {
		}

		public Author(String name, Book book) {
			this.name = name;
			this.book = book;
		}

		public Author(Integer id, String name, Book book) {
			this.id = id;
			this.name = name;
			this.book = book;
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

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Author author = (Author) o;
			return Objects.equals( name, author.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}
}
