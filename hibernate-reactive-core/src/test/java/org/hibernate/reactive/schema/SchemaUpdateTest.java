/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import java.util.concurrent.CompletionStage;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.stage.Stage;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.SQLSERVER;
import static org.hibernate.reactive.containers.DatabaseConfiguration.dbType;

/**
 * Schema update will run different queries when the table already exist
 */
public class SchemaUpdateTest extends BaseReactiveTest {

	@Rule
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( DB2 );

	protected Configuration constructConfiguration(String action) {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( Settings.HBM2DDL_AUTO, action );
		configuration.addAnnotatedClass( BasicTypesTestEntity.class );
		return configuration;
	}

	@Before
	@Override
	public void before(TestContext context) {
		// For these tests we create the factory when we need it
	}

	@After
	@Override
	public void after(TestContext context) {
		super.after( context );
		closeFactory( context );
	}

	/**
	 * Test missing columns creation during schema update
	 */
	@Test
	public void testMissingColumnsCreation(TestContext context) {
		test( context,
			  setupSessionFactory( constructConfiguration( "drop" ) )
					  .thenCompose( v -> getSessionFactory().withTransaction( SchemaUpdateTest::createTable ) )
					  .whenComplete( (u, throwable) -> factoryManager.stop() )
					  .thenCompose( vv -> setupSessionFactory( constructConfiguration( "update" ) )
							  .thenCompose( u -> getSessionFactory().withSession( SchemaUpdateTest::checkAllColumnsExist ) ) )
		);
	}

	/**
	 * Test table creation during schema update
	 */
	@Test
	public void testWholeTableCreation(TestContext context) {
		test( context,
			setupSessionFactory( constructConfiguration( "drop" ) )
				.whenComplete( (u, throwable) -> factoryManager.stop() )
				.thenCompose( v -> setupSessionFactory( constructConfiguration( "update" ) )
					.thenCompose( vv -> getSessionFactory().withSession( SchemaUpdateTest::checkAllColumnsExist ) ) )
		);
	}

	// I don't think it's possible to create a table without columns, so we add
	// a column that's not mapped by the entity
	// We expect the other columns to be created during the update schema phase
	private static CompletionStage<Integer> createTable(Stage.Session session, Stage.Transaction transaction) {
		return session
				.createNativeQuery( "create table " + BasicTypesTestEntity.TABLE_NAME + " (unmapped_column " + columnType() + ")" )
				.executeUpdate();
	}

	private static String columnType() {
		return dbType() == SQLSERVER ? "int" : "integer";
	}

	/**
	 * 	The table is empty, we just want to check that a query runs without errors
	 * 	The query throws an exception if one of the columns has not been created
 	 */
	private static CompletionStage<BasicTypesTestEntity> checkAllColumnsExist(Stage.Session session) {
		return session.find( BasicTypesTestEntity.class, 10 );
	}
}
