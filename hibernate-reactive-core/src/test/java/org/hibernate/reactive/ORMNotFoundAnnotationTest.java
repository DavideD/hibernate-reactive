/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.POSTGRESQL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

/*
 Tests @NotFound annotation with NotFoundAction.EXCEPTION property (i.e. rather than NotFoundAction.IGNORE)
 */
public class ORMNotFoundAnnotationTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( POSTGRESQL );

	private SessionFactory ormFactory;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Painting.class, Dealer.class, Artist.class );
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.DRIVER, "org.postgresql.Driver" );
		configuration.setProperty( Settings.DIALECT, "org.hibernate.dialect.PostgreSQL95Dialect");

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@After
	public void cleanDb(TestContext context) {
		ormFactory.close();
	}

	@Test
	public void testORMWithStageSession(TestContext context) {
		Painting painting = new Painting( 4L, "Mona Lisa" );
		Painting paintingMissingDealer = new Painting( 5L, "Mona Lisa Missing Dealer" );

		Dealer dealer = new Dealer( 2L, "Dealer" );
		Dealer dealerToBeDeleted = new Dealer( 3L,"No one remembers" );
		dealer.addPainting( painting );
		dealerToBeDeleted.addPainting( paintingMissingDealer );

		Artist artist = new Artist( 1L, "Grand Master Painter" );
		artist.addPainting( painting );
		artist.addPainting( paintingMissingDealer );

		/*
		  Setting up the test persisting Artist, Painting, Dealer entities
		      - ALL LOOKS GOOD on the session.find(Artist...) call
		 */
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			session.persist( artist );
			session.persist( dealerToBeDeleted );
			session.persist( painting );
			session.persist( paintingMissingDealer );
			session.getTransaction().commit();

			Artist fullArtist = session.find( Artist.class, artist.id );
			context.assertEquals( 2, fullArtist.paintings.size() );
			context.assertNotNull( fullArtist.paintings.get( 0 ).dealer );
		}

		//  NOTE:  Next I delete one of the Dealer instances and look for the Dealer's and expect only 1 left
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			//  NOTE:  I'm logging the Hibernate Query strings and I don't see ANY "delete from Dealer..." statement
			session.delete( dealerToBeDeleted );
			session.getTransaction().commit();

		/* NOTE:
			- If session.close() is NOT called, then BOTH dealers are deleted and BOTH paintings are deleted
			- If session.close() is called after the delete and then re-open the session
				 ..... then no Dealer is deleted... there's still 2 of them
		 */
//		session.close();
//		session = ormFactory.openSession();
//		session.beginTransaction();

			Artist modifiedArtist = session.find( Artist.class, artist.id );
			Dealer missingDealer = session.find( Dealer.class, dealerToBeDeleted.id );
			Dealer remainingDealer = session.find( Dealer.class, dealer.id );

			context.assertEquals( 2, modifiedArtist.paintings.size() );

			context.assertNull( missingDealer );
			context.assertNotNull( remainingDealer );
			context.assertNotNull( modifiedArtist.paintings.get( 0 ).dealer );
			context.assertNull( modifiedArtist.paintings.get( 1 ).dealer );
		}
	}

	@Entity(name = "Painting")
	@Table(name = "painting")
	public static class Painting {
		@Id
		Long id;
		String name;

		@JoinColumn(nullable = false)
		@ManyToOne(optional = true)
		Artist author;

		@JoinColumn(nullable = true)
		@ManyToOne(optional = true)
		@NotFound(action = NotFoundAction.IGNORE)
		Dealer dealer;

		public Painting() {
		}

		public Painting(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Artist")
	@Table(name = "artist")
	public static class Artist {

		@Id
		Long id;
		String name;

		@OneToMany(mappedBy = "author")
		List<Painting> paintings = new ArrayList<>();

		public Artist() {
		}

		public Artist(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addPainting(Painting painting) {
			this.paintings.add( painting );
			painting.author = this;
		}

	}

	@Entity(name = "Dealer")
	@Table(name = "dealer")
	public static class Dealer {

		@Id
		Long id;
		String name;

		@OneToMany(mappedBy = "dealer")
		List<Painting> paintings = new ArrayList<>();

		public Dealer() {
		}

		public Dealer(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addPainting(Painting painting) {
			this.paintings.add( painting );
			painting.dealer = this;
		}

	}
}
