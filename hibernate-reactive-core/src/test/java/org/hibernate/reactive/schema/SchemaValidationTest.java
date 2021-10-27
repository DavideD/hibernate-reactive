/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.tool.schema.JdbcMetadaAccessStrategy.INDIVIDUALLY;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

/**
 * Test schema validation at startup
 */
public class SchemaValidationTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( DB2 );

	protected Configuration constructConfiguration(String action) {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, INDIVIDUALLY.toString() );
		configuration.setProperty( Settings.HBM2DDL_AUTO, action );
		return configuration;
	}

	@Before
	@Override
	public void before(TestContext context) {
		Configuration createConf = constructConfiguration( "create" );
		createConf.addAnnotatedClass( BasicTypesTestEntity.class );

		// Make sure that the extra table is not in the db
		Configuration dropConf = constructConfiguration( "drop" );
		dropConf.addAnnotatedClass( Extra.class );

		test( context, setupSessionFactory( dropConf )
				.thenCompose( v -> factoryManager.stop() )
				.thenCompose( v -> setupSessionFactory( createConf ) )
				.thenCompose( v -> factoryManager.stop() )
		);
	}

	@After
	@Override
	public void after(TestContext context) {
		super.after( context );
		closeFactory( context );
	}

	@Test
	public void testValidationSucceeds(TestContext context) {
		Configuration validateConf = constructConfiguration( "validate" );
		validateConf.addAnnotatedClass( BasicTypesTestEntity.class );
		test( context, setupSessionFactory( validateConf ) );
	}

	@Test
	public void testValidationFails(TestContext context) {
		Configuration validateConf = constructConfiguration( "validate" );
		validateConf.addAnnotatedClass( BasicTypesTestEntity.class );
		// The table mapping this entity shouldn't be in the db
		validateConf.addAnnotatedClass( Extra.class );

		final String errorMessage = "Schema-validation: missing table [" + Extra.TABLE_NAME + "]";
		test( context, setupSessionFactory( validateConf )
				.handle( (unused, throwable) -> {
					context.assertNotNull( throwable );
					context.assertEquals( throwable.getClass(), SchemaManagementException.class );
					context.assertEquals( throwable.getMessage(), errorMessage );
					return null;
				} )
		);
	}

	/**
	 * An extra entity used for validation,
	 * it should not be created at start up
	 */
	@Entity(name = "Extra")
	@Table(name = Extra.TABLE_NAME)
	public static class Extra {
		public static final String TABLE_NAME = "EXTRA_TABLE";
		@Id
		@GeneratedValue
		private Integer id;

		private String description;
	}
}
