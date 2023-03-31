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
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.COCKROACHDB;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;
import static org.hibernate.reactive.provider.Settings.DIALECT;
import static org.hibernate.reactive.provider.Settings.DRIVER;

/**
 * This test class verifies that data can be persisted and queried on the same database
 * using both JPA/hibernate and reactive session factories.
 */
public class ORMReactivePersistenceTest extends BaseReactiveTest {

	// DB2: The CompletionStage test throw java.lang.IllegalStateException: Needed to have 6 in buffer...
	// Cockroach: We need to change the URL schema we normally use for testing
	@Rule
	public DatabaseSelectionRule skip = DatabaseSelectionRule.skipTestsFor( DB2, COCKROACHDB );

	private SessionFactory ormFactory;

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Flour.class );
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( DRIVER, dbType().getJdbcDriver() );
		configuration.setProperty( DIALECT, dbType().getDialectClass().getName() );
		configuration.setProperty( SHOW_SQL, "true" );
		configuration.setProperty( AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
		configuration.addAnnotatedClass( EntityWithIdentity.class );

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
	public void testORMWithStageSession(TestContext context) {
		EntityWithIdentity entityWithIdentity = new EntityWithIdentity( 12 );
		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			session.persist( entityWithIdentity );
			session.getTransaction().commit();
		}

		try (Session session = ormFactory.openSession()) {
			session.beginTransaction();
			EntityWithIdentity entityWithIdentity1 = session.find( EntityWithIdentity.class, entityWithIdentity.id );
			session.getTransaction().commit();
		}
	}

	@Entity(name = "EntityWithIdentity")
	private static class EntityWithIdentity {
		private static final String PREFIX = "Entity: ";
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		@Column(unique = true)
		String name;

		@Column
		private int position;

		public EntityWithIdentity() {
		}

		public EntityWithIdentity(int index) {
			this.name =  PREFIX + index;
			this.position = index;
		}

		public int getPosition() {
			return position;
		}

		public void setPosition(int position) {
			this.position = position;
		}

		@Override
		public String toString() {
			return id + ":" + name + ":" + position;
		}
	}

	@Entity(name = "Flour")
	@Table(name = "Flour")
	public static class Flour {
		@Id
		private Integer id;
		private String name;
		private String description;
		private String type;

		public Flour() {
		}

		public Flour(Integer id, String name, String description, String type) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.type = type;
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

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Flour flour = (Flour) o;
			return Objects.equals( name, flour.name ) &&
					Objects.equals( description, flour.description ) &&
					Objects.equals( type, flour.type );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name, description, type );
		}
	}
}
