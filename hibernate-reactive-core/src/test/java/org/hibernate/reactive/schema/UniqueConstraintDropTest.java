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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

public class UniqueConstraintDropTest extends BaseReactiveTest {

	public static class IndividuallyUniqueConstraintDropTestBase extends UniqueConstraintDropTest {

		@Override
		protected Configuration constructConfiguration(String hbm2DdlOption) {
			final Configuration configuration = super.constructConfiguration( hbm2DdlOption );
			configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, INDIVIDUALLY.toString() );
			return configuration;
		}
	}

	public static class GroupedUniqueConstraintDropTestBase extends UniqueConstraintDropTest {

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
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( MARIA, COCKROACHDB, DB2 );

	@Before
	@Override
	public void before(TestContext context) {

		Configuration configuration = constructConfiguration( "create" );
		configuration.addAnnotatedClass( CustomerFirst.class );
		configuration.setProperty( Settings.SHOW_SQL, System.getProperty(Settings.SHOW_SQL, "true") );

		test( context, setupSessionFactory( configuration )
				.thenCompose( v -> factoryManager.stop() ) );
	}

	@After
	@Override
	public void after(TestContext context) {

		final Configuration configuration = constructConfiguration( "drop" );
		configuration.addAnnotatedClass( CustomerSecond.class );

		test( context, factoryManager.stop()
				.thenCompose( v -> setupSessionFactory( configuration ) )
				.thenCompose( v -> factoryManager.stop() ) );
	}

	@Test
	public void testValidationSucceed(TestContext context) {
		Configuration configuration = constructConfiguration( "validate" );
		configuration.addAnnotatedClass( CustomerFirst.class );

		test(
				context,
				setupSessionFactory( configuration )
						.thenCompose( v -> getSessionFactory()
								.withTransaction(
										(session, t) -> session.createQuery( "FROM Customer", CustomerFirst.class )
												.getResultList() ) )
						.thenAccept( results -> context.assertTrue( results.isEmpty() ) )
		);
	}

	@Test
	public void testDropUniqueConstraint(TestContext context) throws Exception {
		final CustomerSecond secondCustomer = new CustomerSecond();
		secondCustomer.customerId = "Second Customer";

		final String constraintInfoQueryPG =
				"SELECT constraint_name FROM information_schema.key_column_usage where table_name = 'customer'";

		final Configuration configuration = constructConfiguration( "update" );
		configuration.addAnnotatedClass( CustomerSecond.class );

		test(
				context,
				setupSessionFactory( configuration )
						.thenCompose( v -> getSessionFactory()
								.withTransaction( (session, t) -> session.persist( secondCustomer ) ) )
						.thenCompose( v1 -> openSession()
								.thenCompose( s -> s
										.find( CustomerSecond.class, secondCustomer.customerAccountNumber )
										.thenAccept( result -> context.assertNotNull( result ) )
										.thenCompose( v -> s.createNativeQuery( constraintInfoQueryPG, String.class )
												.getResultList()
												.thenAccept( list -> {
													context.assertEquals( 1, list.size() );
													context.assertTrue( list.get( 0 ).equals( "customer_pkey" ));
												} )
										)
								)
						)
		);
	}

	@Entity(name = "Customer")
	public static class CustomerFirst {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "CUSTOMER_ACCOUNT_NUMBER")
		public Long customerAccountNumber;

		@Basic
		@Column(name = "CUSTOMER_ID", unique = true)
		public String customerId;

		@Basic
		@Column(name = "BILLING_ADDRESS")
		public String billingAddress;

		public CustomerFirst() {}
	}

	@Entity(name = "Customer")
	public static class CustomerSecond {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "CUSTOMER_ACCOUNT_NUMBER")
		public Long customerAccountNumber;

		@Basic
		@Column(name = "CUSTOMER_ID")
		public String customerId;

		@Basic
		@Column(name = "BILLING_ADDRESS")
		public String billingAddress;

		public CustomerSecond() {}
	}
}
