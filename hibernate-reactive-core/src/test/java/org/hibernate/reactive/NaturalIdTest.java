/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.vertx.ext.unit.TestContext;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.reactive.provider.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.reactive.common.Identifier.composite;
import static org.hibernate.reactive.common.Identifier.id;

public class NaturalIdTest extends BaseReactiveTest {

	private SessionFactory ormFactory;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( SimpleThing.class, CompoundThing.class );
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.DRIVER, "org.postgresql.Driver" );
		configuration.setProperty( Settings.DIALECT, PostgreSQLDialect.class.getName() );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@After
	public void closeOrmFactory() {
		ormFactory.close();
	}

	@Test
	public void testORMSimpleNaturalId(TestContext context) {
		SimpleThing thing1 = new SimpleThing();
		thing1.naturalKey = "xyz666";

		Session session = ormFactory.openSession();
		session.beginTransaction();

		session.persist( thing1 );

		session.getTransaction().commit();
		session.close();

		session = ormFactory.openSession();
		session.beginTransaction();

		SimpleThing thing = session.bySimpleNaturalId( SimpleThing.class ).load( "xyz666" );
		Assertions.assertThat(thing).isNotNull();

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testORMCompoundNaturalId(TestContext context) {
		CompoundThing thing1 = new CompoundThing();
		thing1.naturalKey = "xyz666";
		thing1.version = 1;
		CompoundThing thing2 = new CompoundThing();
		thing2.naturalKey = "xyz666";
		thing2.version = 2;

		try (Session session = ormFactory.openSession() ) {
			session.beginTransaction();

			session.persist( thing1 );
			session.persist( thing2 );

			session.getTransaction().commit();
		}

		try (Session session = ormFactory.openSession() ) {
			session.beginTransaction();
			CompoundThing thing = session.bySimpleNaturalId( CompoundThing.class )
					.load( Map.of(
							"naturalKey",
							thing1.naturalKey,
							"version",
							thing1.version
					) );
			Assertions.assertThat( thing ).isEqualTo( thing1 );
			session.getTransaction().commit();
		}
	}

	@Test
	public void testSimpleNaturalId(TestContext context) {
		SimpleThing thing1 = new SimpleThing();
		thing1.naturalKey = "abc123";
		SimpleThing thing2 = new SimpleThing();
		thing2.naturalKey = "def456";
		test(
				context,
				getSessionFactory()
						.withSession( session -> session.persist( thing1, thing2 ).thenCompose( v -> session.flush() ) )
						.thenCompose( v -> getSessionFactory().withSession(
								session -> session.find( SimpleThing.class, id( "naturalKey", "abc123" ) )
						) )
						.thenAccept( t -> {
							context.assertNotNull( t );
							context.assertEquals( thing1.id, t.id );
						} )
						.thenCompose( v -> getSessionFactory().withSession(
								session -> session.find( SimpleThing.class, id( SimpleThing.class, "naturalKey", "not an id" ) )
						) )
						.thenAccept( context::assertNull )
		);
	}

	@Test
	public void testCompoundNaturalId(TestContext context) {
		CompoundThing thing1 = new CompoundThing();
		thing1.naturalKey = "xyz666";
		thing1.version = 1;
		CompoundThing thing2 = new CompoundThing();
		thing2.naturalKey = "xyz666";
		thing2.version = 2;
		test(
				context,
				getSessionFactory()
						.withSession( session -> session.persist( thing1, thing2 ).thenCompose( v -> session.flush() ) )
						.thenCompose( v -> getSessionFactory().withSession(
								session -> session.find( CompoundThing.class, composite(
										id( "naturalKey", "xyz666" ),
										id( "version", 1L )
								) )
						) )
						.thenAccept( t -> {
							context.assertNotNull( t );
							context.assertEquals( thing1.id, t.id );
						} )
						.thenCompose( v -> getSessionFactory().withSession(
								session -> session.find( CompoundThing.class, composite(
										id( CompoundThing.class, "naturalKey", "xyz666" ),
										id( CompoundThing.class, "version", 3 )
								) )
						) )
						.thenAccept( context::assertNull )
		);
	}



	@Entity(name = "SimpleThing")
	static class SimpleThing {
		@Id
		@GeneratedValue
		long id;
		@NaturalId
		String naturalKey;
	}

	@Entity(name = "CompoundThing")
	static class CompoundThing {
		@Id
		@GeneratedValue
		long id;
		@NaturalId
		String naturalKey;
		@NaturalId
		int version;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CompoundThing that = (CompoundThing) o;
			return version == that.version && Objects.equals( naturalKey, that.naturalKey );
		}

		@Override
		public int hashCode() {
			return Objects.hash( naturalKey, version );
		}
	}
}
