/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.annotations.DisabledFor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.COCKROACHDB;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;
import static org.hibernate.reactive.provider.Settings.DIALECT;
import static org.hibernate.reactive.provider.Settings.DRIVER;

@Timeout(value = 10, timeUnit = MINUTES)
/**
 * This test class verifies that data can be persisted and queried on the same database
 * using both JPA/hibernate and reactive session factories.
 */
@DisabledFor(value = DB2, reason = "Exception: IllegalStateException: Needed to have 6 in buffer but only had 0")
@DisabledFor(value = COCKROACHDB, reason = "We need to change the URL schema we normally use for testing")
public class ORMReactivePersistenceTest extends BaseReactiveTest {

	private SessionFactory ormFactory;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( SimpleThing.class );
	}

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

	@Test
	public void testORMWithStageSession() {
		SimpleThing thing1 = new SimpleThing();
		thing1.naturalKey = "abc123";
		SimpleThing thing2 = new SimpleThing();
		thing2.naturalKey = "def456";

		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			session.persist( thing1 );
			session.persist( thing2 );
			session.getTransaction().commit();
		}

		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			SimpleThing found = session
					.byNaturalId( SimpleThing.class )
					.using( "naturalKey", "abc123" )
					.load();
			assertThat( found ).isEqualTo( thing1 );
			session.getTransaction().commit();
		}
	}

	@Entity(name = "SimpleThing")
	static class SimpleThing {
		@Id
		@GeneratedValue
		long id;
		@NaturalId
		String naturalKey;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			SimpleThing that = (SimpleThing) o;
			return Objects.equals( naturalKey, that.naturalKey );
		}

		@Override
		public int hashCode() {
			return Objects.hash( naturalKey );
		}
	}
}
