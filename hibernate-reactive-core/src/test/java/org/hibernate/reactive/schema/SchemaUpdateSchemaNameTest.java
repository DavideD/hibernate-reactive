/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.DB2;
import static org.hibernate.tool.schema.JdbcMetadaAccessStrategy.GROUPED;
import static org.hibernate.tool.schema.JdbcMetadaAccessStrategy.INDIVIDUALLY;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.BaseReactiveTest;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.vertx.ext.unit.TestContext;

public abstract class SchemaUpdateSchemaNameTest extends BaseReactiveTest {

	public static class IndividuallySchemaUpdateSchemaNameTestBase extends SchemaUpdateSchemaNameTest {

		@Override
		protected Configuration constructConfiguration(String hbm2DdlOption) {
			final Configuration configuration = super.constructConfiguration( hbm2DdlOption );
			configuration.setProperty( Settings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, INDIVIDUALLY.toString() );
			return configuration;
		}
	}

	public static class GroupedSchemaUpdateSchemaNameTestBase extends SchemaUpdateSchemaNameTest {

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
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.skipTestsFor( DB2 );

	@Before
	@Override
	public void before(TestContext context) {
		Configuration createHbm2ddlConf = constructConfiguration( "create" );
		createHbm2ddlConf.addAnnotatedClass( SimpleEntityFirst.class );

		test( context, setupSessionFactory( createHbm2ddlConf )
				.thenCompose( v -> factoryManager.stop() ) );
	}

	@After
	@Override
	public void after(TestContext context) {
		final Configuration dropHbm2ddlConf = constructConfiguration( "drop" );
		dropHbm2ddlConf.addAnnotatedClass( SimpleEntityNext.class );

		test( context, factoryManager.stop()
				.thenCompose( v -> setupSessionFactory( dropHbm2ddlConf ) )
				.thenCompose( v -> factoryManager.stop() ) );
	}

	@Test
	public void testSqlAlterWithTableSchemaName(TestContext context) throws Exception {

		final Configuration configuration = constructConfiguration( "update" );
		configuration.addAnnotatedClass( SimpleEntityNext.class );

		test(
				context,
				setupSessionFactory( configuration )
						.thenCompose( v -> getSessionFactory()
								.withTransaction( (session, t) -> session.createQuery( "FROM SimpleEntity", SimpleEntityNext.class )
										.getResultList() ) )
						.thenAccept( results -> context.assertTrue( results.isEmpty() ) )
		);
	}

	@Test
	public void testPersistenceOfAlteredSchema(TestContext context) throws Exception {
		final SimpleEntityNext aSimpleEntity = new SimpleEntityNext();
		aSimpleEntity.id = 9;
		aSimpleEntity.setValue( 99 );

		final Configuration configuration = constructConfiguration( "update" );
		configuration.addAnnotatedClass( SimpleEntityNext.class );

		test(
				context,
				setupSessionFactory( configuration )
						.thenCompose( v -> getSessionFactory()
								.withTransaction( (session, t) -> session.persist( aSimpleEntity ) ) )
						.thenCompose( v1 -> openSession()
								.thenCompose( s -> s
										.find( SimpleEntityNext.class, aSimpleEntity.id )
										.thenAccept( result -> {
											context.assertNotNull( result );
											context.assertEquals( aSimpleEntity.getValue(), result.getValue() );
											context.assertEquals( aSimpleEntity.data, result.data );
										}
									)
								)
						)
		);
	}

	@MappedSuperclass
	public static abstract class AbstractSimpleEntity {
		@Id
		protected Integer id;
		private Integer value;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity") //, schema = "test")
	public static class SimpleEntityFirst extends AbstractSimpleEntity {

	}

	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity") //, schema = "test")
	public static class SimpleEntityNext extends AbstractSimpleEntity {
		private String data;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
