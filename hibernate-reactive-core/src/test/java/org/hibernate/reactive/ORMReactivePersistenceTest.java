/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.reactive.annotations.DisabledFor;
import org.hibernate.reactive.quarkus.User;
import org.hibernate.reactive.quarkus.WebAuthnCredential;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;

import static java.util.concurrent.TimeUnit.MINUTES;
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
		return List.of( User.class, WebAuthnCredential.class );
	}

	@BeforeEach
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( DRIVER, dbType().getJdbcDriver() );
		configuration.setProperty( DIALECT, dbType().getDialectClass().getName() );

		configuration.setProperty( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 16 );
		configuration.setProperty( "hibernate.batch_fetch_style", BatchFetchStyle.PADDED );
		configuration.setProperty( "hibernate.batch_fetch_style", "" );
		configuration.setProperty( "hibernate.order_by.default_null_ordering", "none" );
		configuration.setProperty( "hibernate.cache.use_reference_entries", Boolean.TRUE );
		configuration.setProperty( "hibernate.query.in_clause_parameter_padding", "true" );

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
	public void testORMWitMutinySession() {
		WebAuthnCredential.RequiredPersistedData data = new WebAuthnCredential.RequiredPersistedData(
				"Azel",
				"credential-id-unique-byte-sequence",
				UUID.randomUUID(),
				"very-safe-public-key".getBytes(),
				-7L,
				333L
		);

		final Long id;
		try (Session ormSession = ormFactory.openSession()) {
			ormSession.getTransaction().begin();
			User user = new User();
			user.username = data.username();
			ormSession.persist( user );
			id = user.id;
			ormSession.getTransaction().commit();
		}

		try (Session ormSession = ormFactory.openSession()) {
			WebAuthnCredential credential = new WebAuthnCredential( data );
			credential.setUser( ormSession.getReference( User.class, id ) );

			ormSession.beginTransaction();
			ormSession.persist( credential );
			ormSession.getTransaction().commit();
		}
	}
}
