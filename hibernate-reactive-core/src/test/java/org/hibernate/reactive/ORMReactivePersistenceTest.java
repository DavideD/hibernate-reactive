/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.reactive.provider.Settings;

import org.junit.Test;

import io.vertx.ext.unit.TestContext;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.tool.schema.JdbcMetadaAccessStrategy.INDIVIDUALLY;

/**
 * This test class verifies that data can be persisted and queried on the same database
 * using both JPA/hibernate and reactive session factories.
 */
public class ORMReactivePersistenceTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Flour.class );
	}

	private SessionFactory createORMFactory(String action) {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.DRIVER, "com.mysql.cj.jdbc.Driver" );
		configuration.setProperty( Settings.DIALECT, MySQLDialect.class.getName() );
		configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, INDIVIDUALLY.toString() );
		configuration.setProperty( AvailableSettings.HBM2DDL_AUTO, action );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );
		StandardServiceRegistry registry = builder.build();
		return configuration.buildSessionFactory( registry );
	}

	@Test
	public void testORMWithStageSession(TestContext context) {
		try (SessionFactory ormFactory = createORMFactory( "create" )) {
		}
		try (SessionFactory ormFactory = createORMFactory( "validate" )) {
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

		@Convert(converter = org.hibernate.type.YesNoConverter.class)
		private Boolean booleanTrueFalse;

		@Convert(converter = org.hibernate.type.TrueFalseConverter.class)
		private Boolean booleanYesNo;

		@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
		private Boolean booleanNumeric;

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
