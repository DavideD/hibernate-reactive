/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.configuration;

import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.H2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.impl.H2SqlClientPool;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPoolConfiguration;
import org.hibernate.reactive.pool.impl.SqlClientPoolConfiguration;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;
import org.hibernate.reactive.testing.TestingRegistryRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class H2ReactiveConnectionPoolTest {

	@Rule
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.runOnlyFor( H2 );

	@Rule
	public Timeout rule = Timeout.seconds( 3600 );

	@Rule
	public TestingRegistryRule registryRule = new TestingRegistryRule();

	@Rule
	public RunTestOnContext vertxContextRule = new RunTestOnContext();

	protected static void test(TestContext context, CompletionStage<?> cs) {
		// this will be added to TestContext in the next vert.x release
		Async async = context.async();
		cs.whenComplete( (res, err) -> {
			if ( err != null ) {
				context.fail( err );
			}
			else {
				async.complete();
			}
		} );
	}

	private ReactiveConnectionPool configureAndStartPool(Map<String, Object> config) {
		DefaultSqlClientPoolConfiguration poolConfig = new DefaultSqlClientPoolConfiguration();
		poolConfig.configure( config );
		registryRule.addService( SqlClientPoolConfiguration.class, poolConfig );
		registryRule.addService( JdbcServices.class, new JdbcServicesImpl() {
			@Override
			public SqlStatementLogger getSqlStatementLogger() {
				return new SqlStatementLogger();
			}
		} );
		H2SqlClientPool reactivePool = new H2SqlClientPool();
		reactivePool.injectServices( registryRule.getServiceRegistry() );
		reactivePool.configure( config );
		reactivePool.start();
		return reactivePool;
	}

	@Test
	public void configureWithJdbcUrl(TestContext context) {
		String url = "jdbc:h2:~/test";
		Map<String,Object> config = new HashMap<>();
		config.put( Settings.URL, url );
		ReactiveConnectionPool reactivePool = configureAndStartPool( config );
		test( context, verifyConnectivity( context, reactivePool ) );
	}

	private CompletionStage<Void> verifyConnectivity(TestContext context, ReactiveConnectionPool reactivePool) {
		return reactivePool.getConnection().thenCompose(
				connection -> connection.select( "SELECT 1" )
						.thenAccept( rows -> {
							context.assertNotNull( rows );
							context.assertEquals( 1, rows.size() );
							context.assertEquals( 1, rows.next()[0] );
						} ) );
	}
}
