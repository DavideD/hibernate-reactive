/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.ORACLE;

/**
 * A temporary utility test class to compare the behaviour with ORM
 */
public class ORMTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule rule = DatabaseSelectionRule.runOnlyFor( ORACLE );

	private SessionFactory ormFactory;

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.addAnnotatedClass( Flour.class );
		return configuration;
	}

	@Before
	public void prepareOrmFactory() {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.DRIVER, "oracle.jdbc.OracleDriver" );
		configuration.setProperty( Settings.DIALECT, Oracle12cDialect.class.getName() );
		configuration.setProperty( AvailableSettings.USE_GET_GENERATED_KEYS, "true" );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );

		StandardServiceRegistry registry = builder.build();
		ormFactory = configuration.buildSessionFactory( registry );
	}

	@After
	public void cleanDb(TestContext context) {
		ormFactory.close();

		test( context, deleteEntities( "Flour" ) );
	}

	@Test
	public void testORMWithStageSession() {
		final Flour almond = new Flour( null, "Almond", "made from ground almonds.", "Gluten free" );

		Session session = ormFactory.openSession();
		session.beginTransaction();
		session.persist( almond );
		assert almond.getId() != null;
		session.getTransaction().commit();
		session.close();
	}

	@Entity(name = "Flour")
	@Table(name = "Flour")
	public static class Flour {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
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