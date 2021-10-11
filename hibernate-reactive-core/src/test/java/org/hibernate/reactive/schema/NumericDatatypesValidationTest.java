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

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

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

public abstract class NumericDatatypesValidationTest  extends BaseReactiveTest implements TestDBDatatypeProvider {

	public static class IndividuallyNumericDatatypesValidationTestBase extends NumericDatatypesValidationTest {

		@Override
		protected Configuration constructConfiguration(String hbm2DdlOption) {
			final Configuration configuration = super.constructConfiguration( hbm2DdlOption );
			configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, INDIVIDUALLY.toString() );
			return configuration;
		}
	}

	public static class GroupedNumericDatatypesValidationTestBase extends NumericDatatypesValidationTest {

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

	@Rule
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( MARIA, COCKROACHDB, DB2);

	@Before
	@Override
	public void before(TestContext context) {

		Configuration configuration = constructConfiguration( "create" );
		configuration.addAnnotatedClass( TestEntity.class );
		configuration.setProperty( Settings.SHOW_SQL, System.getProperty(Settings.SHOW_SQL, "true") );

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

	@Test
	public void testNumericDataTypes(TestContext context) {
		final TestEntity testEntity = new TestEntity();
		testEntity.id = 9;
		testEntity.bigDecimalValue = BigDecimal.valueOf( 3.14159 );
		testEntity.longValue = 4L;
		testEntity.integerValue = 4;
		testEntity.doubleValue = 4.0;
		testEntity.floatValue = 4F;

		final Configuration configuration = constructConfiguration( "update" );
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
													context.assertEquals(
															testEntity.bigDecimalValue, testEntity.bigDecimalValue );
												}
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aBigDecimalColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.BIGDECIMAL
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aLongColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.LONG
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aIntegerColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.INTEGER
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aDoubleColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.DOUBLE
												) )
										).thenCompose( v -> s.createNativeQuery(
														getDatatypeQuery( "TestEntity", "aFloatColumn" ), String.class )
												.getResultList()
												.thenAccept( resultList -> assertDatatype( context, resultList,
														DATATYPE.FLOAT
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
		@Id
		public Integer id;

		@Column(name = "aBigDecimalColumn")
		BigDecimal bigDecimalValue;

		@Column(name = "aLongColumn")
		Long longValue;

		@Column(name = "aIntegerColumn")
		Integer integerValue;

		@Column(name = "aDoubleColumn")
		Double doubleValue;

		@Column(name = "aFloatColumn")
		Float floatValue;
	}
}
