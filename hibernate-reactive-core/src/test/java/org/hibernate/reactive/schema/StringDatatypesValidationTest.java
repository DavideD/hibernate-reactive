/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.COCKROACHDB;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.MARIA;
import static org.hibernate.tool.schema.JdbcMetadaAccessStrategy.GROUPED;
import static org.hibernate.tool.schema.JdbcMetadaAccessStrategy.INDIVIDUALLY;

import java.io.Serializable;
import java.sql.Blob;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.containers.DatabaseConfiguration;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

public abstract class StringDatatypesValidationTest extends BaseReactiveTest implements TestDBDatatypeProvider {

	public static class IndividuallyStringDatatypesValidationTestBase extends StringDatatypesValidationTest {

		@Override
		protected Configuration constructConfiguration(String hbm2DdlOption) {
			final Configuration configuration = super.constructConfiguration( hbm2DdlOption );
			configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, INDIVIDUALLY.toString() );
			return configuration;
		}
	}

	public static class GroupedStringDatatypesValidationTestBase extends StringDatatypesValidationTest {

		@Override
		protected Configuration constructConfiguration(String hbm2DdlOption) {
			final Configuration configuration = super.constructConfiguration( hbm2DdlOption );
			configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, GROUPED.toString() );
			return configuration;
		}
	}

	protected Configuration constructConfiguration(String hbm2DdlOption) {
		Configuration configuration = constructConfiguration();
		configuration.setProperty( Settings.HBM2DDL_AUTO, hbm2DdlOption );
		return configuration;
	}

	@Before
	@Override
	public void before(TestContext context) {
		Configuration configuration = constructConfiguration( "create" );
		configuration.addAnnotatedClass( TestEntity.class );

		test( context, setupSessionFactory( configuration )
				.thenCompose( v -> factoryManager.stop() ) );
	}

	@After
	@Override
	public void after(TestContext context) {
		final Configuration configuration = constructConfiguration( "drop" );
		configuration.addAnnotatedClass( TestEntity.class );

		test( context, factoryManager.stop()
				.thenCompose( v -> setupSessionFactory( configuration ) )
				.thenCompose( v -> factoryManager.stop() ) );
	}

	@Rule
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( MARIA, COCKROACHDB, DB2 );

	/**
	 * Register the TestEntity and perform validation on session creation
	 */
	@Test
	public void testValidationSucceed(TestContext context) {
		Configuration configuration = constructConfiguration( "validate" );
		configuration.addAnnotatedClass( TestEntity.class );

		test(
				context,
				setupSessionFactory( configuration )
						.thenCompose( v -> getSessionFactory()
								.withTransaction(
										(session, t) -> session.createQuery( "FROM TestEntity", TestEntity.class )
												.getResultList() ) )
						.thenAccept( results -> context.assertTrue( results.isEmpty() ) )
		);
	}

	/**
	 * Create instance of test entity
	 */
	@Test
	public void testCharacterDataTypes(TestContext context) {
		final TestEntity testEntity = new TestEntity();
		testEntity.id = 9;
		testEntity.sampleText = "sample string of text";

		final Configuration configuration = constructConfiguration( "create" );
		configuration.addAnnotatedClass( TestEntity.class );

		test(
				context,
				setupSessionFactory( configuration )
						.thenCompose( v -> getSessionFactory()
								.withTransaction( (session, t) -> session.persist( testEntity ) ) )
						.thenCompose( v1 -> openSession()
								.thenCompose( s -> s
										.find( TestEntity.class, testEntity.id )
										.thenAccept( result -> {
													context.assertNotNull( result );
													context.assertEquals( testEntity.sampleText, testEntity.sampleText );
												}
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aTextColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.TEXT
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aBlobColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.BLOB
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aBooleanColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.BOOLEAN
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aSerializableColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.SERIALIZABLE
												) )
										)
										.thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aCharacterColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.CHARACTER
												) )
										)
								)
						)
		);
	}

	private String getDatatypeQuery( String actualTableName, String actualColumnName ) {
		return TestDBDatatypeProvider.getDatatypeQuery(
				DatabaseConfiguration.dbType(),
				actualTableName,
				actualColumnName
		);
	}

	private String getExpectedResult( DATATYPE datatype ) {
		return TestDBDatatypeProvider.getExpectedResult( datatype, DatabaseConfiguration.dbType() );
	}

	private  void assertDatatype(TestContext context, List<String> results, DATATYPE datatype) {
		context.assertEquals( 1, results.size() );
		context.assertTrue( results.get( 0 ).equals( TestDBDatatypeProvider.getExpectedResult( datatype, DatabaseConfiguration.dbType() ) ) );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		public static final int BLOB_LENGTH = 100000000;

		@Id
		public Integer id;

		@Type(type = "text")
		@Column(name = "aTextColumn")
		String sampleText;

		@Column(name = "aBooleanColumn")
		Boolean booleanValue;

		@Column(name = "aBlobColumn", length = BLOB_LENGTH)
		private Blob blobValue;

		@Column(name = "aSerializableColumn")
		private Serializable serializableValue;

		@Column(name = "aCharacterColumn")
		Character sampleChar;
	}
}
