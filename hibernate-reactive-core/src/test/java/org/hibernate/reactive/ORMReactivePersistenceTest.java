/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.reactive.annotations.DisabledFor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.junit5.Timeout;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.COCKROACHDB;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;
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
		return List.of( GeneratedWithIdentity.class );
	}

	@BeforeEach
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( DRIVER, dbType().getJdbcDriver() );
		configuration.setProperty( AvailableSettings.HBM2DDL_CREATE_SOURCE, "script-then-metadata");
		configuration.setProperty(AvailableSettings.HBM2DDL_CREATE_SCRIPT_SOURCE, "/mysql-pipe.sql");

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
		final GeneratedWithIdentity davide = new GeneratedWithIdentity( "Davide", "D'Alto" );

		CurrentUser.INSTANCE.logIn( "dd-insert" );
		try (Session ormSession = ormFactory.openSession()) {
			ormSession.beginTransaction();
			ormSession.persist( davide );
			ormSession.getTransaction().commit();
		}
		org.hibernate.reactive.CurrentUser.INSTANCE.logOut();
	}

	public static class CurrentUser {

		public static final CurrentUser INSTANCE = new CurrentUser();

		private static final ThreadLocal<String> storage = new ThreadLocal<>();

		public void logIn(String user) {
			storage.set( user );
		}

		public void logOut() {
			storage.remove();
		}

		public String get() {
			return storage.get();
		}


		@ValueGenerationType(generatedBy = InsertLoggedUserGenerator.class)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface LoggedUserInsert {}

		@ValueGenerationType(generatedBy = AlwaysLoggedUserGenerator.class)
		@Retention(RetentionPolicy.RUNTIME)
		public @interface LoggedUserAlways {}

		public static abstract class AbstractLoggedUserGenerator implements BeforeExecutionGenerator {
			@Override
			public Object generate(
					SharedSessionContractImplementor session,
					Object owner,
					Object currentValue,
					EventType eventType) {
				return CurrentUser.INSTANCE.get();
			}
		}

		public static class InsertLoggedUserGenerator extends AbstractLoggedUserGenerator {

			@Override
			public EnumSet<EventType> getEventTypes() {
				return EventTypeSets.INSERT_ONLY;
			}
		}

		public static class AlwaysLoggedUserGenerator extends AbstractLoggedUserGenerator {

			@Override
			public EnumSet<EventType> getEventTypes() {
				return EventTypeSets.ALL;
			}
		}
	}

	@Entity(name = "GeneratedWithIdentity")
	@Table(name = "GeneratedWithIdentitySingleTable")
	static class GeneratedWithIdentity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		public String firstname;

		public String lastname;

		@Generated(GenerationTime.ALWAYS)
		@Column(columnDefinition = "varchar(600) generated always as (firstname || ' ' || lastname) stored")
		private String fullName;

		@Temporal(value = TemporalType.TIMESTAMP)
		@Generated(GenerationTime.INSERT)
		@Column(columnDefinition = "timestamp")
		@ColumnDefault("current_timestamp")
		public Date createdAt;

		@CurrentUser.LoggedUserInsert
		public String createdBy;

		@CurrentUser.LoggedUserAlways
		public String updatedBy;

		@Generated(GenerationTime.NEVER)
		public String never;

		public GeneratedWithIdentity() {
		}

		public GeneratedWithIdentity(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}
}
