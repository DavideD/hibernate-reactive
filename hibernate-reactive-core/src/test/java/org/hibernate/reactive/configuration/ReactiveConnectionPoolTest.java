/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.containers.DatabaseConfiguration;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPool;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPoolConfiguration;
import org.hibernate.reactive.pool.impl.SqlClientPoolConfiguration;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.testing.DatabaseSelectionRule;
import org.hibernate.reactive.testing.TestingRegistryRule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.vertx.core.VertxOptions;
import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.containers.DatabaseConfiguration.DBType.POSTGRESQL;
import static org.hibernate.reactive.testing.ReactiveAssertions.assertThrown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactiveConnectionPoolTest {

	@RegisterExtension
	public DatabaseSelectionRule dbRule = DatabaseSelectionRule.runOnlyFor( POSTGRESQL );

//	@RegisterExtension
//	public Timeout rule = Timeout.seconds( 3600 );

	@RegisterExtension
	public TestingRegistryRule registryRule = new TestingRegistryRule();

	@RegisterExtension
	static RunTestOnContext testOnContext = new RunTestOnContext( new VertxOptions());

	protected static void test(VertxTestContext context, CompletionStage<?> cs) {
		// this will be added to TestContext in the next vert.x release
		cs.whenComplete( (res, err) -> {
			if ( err != null ) {
				context.failNow( err );
			}
			else {
				context.completeNow();
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
		DefaultSqlClientPool reactivePool = new DefaultSqlClientPool();
		reactivePool.injectServices( registryRule.getServiceRegistry() );
		reactivePool.configure( config );
		reactivePool.start();
		return reactivePool;
	}

	@Test
	public void configureWithJdbcUrl(VertxTestContext context) {
		String url = DatabaseConfiguration.getJdbcUrl();
		Map<String,Object> config = new HashMap<>();
		config.put( Settings.URL, url );
		ReactiveConnectionPool reactivePool = configureAndStartPool( config );
		test( context, verifyConnectivity( context, reactivePool ) );
	}

	@Test
	public void configureWithCredentials(VertxTestContext context) {
		// Set up URL with invalid credentials so we can ensure that
		// explicit USER and PASS settings take precedence over credentials in the URL
		String url = DatabaseConfiguration.getJdbcUrl();
		url = url.replace( "user=" + DatabaseConfiguration.USERNAME, "user=bogus" );
		url = url.replace( "password=" + DatabaseConfiguration.PASSWORD, "password=bogus" );

		// Correct user/password are supplied explicitly in the config map and
		// should override the credentials in the URL
		Map<String,Object> config = new HashMap<>();
		config.put( Settings.URL, url );
		config.put( Settings.USER, DatabaseConfiguration.USERNAME );
		config.put( Settings.PASS, DatabaseConfiguration.PASSWORD );
		ReactiveConnectionPool reactivePool = configureAndStartPool( config );
		test( context, verifyConnectivity( context, reactivePool ) );
	}

	@Test
	public void configureWithWrongCredentials(VertxTestContext context) {
		String url = DatabaseConfiguration.getJdbcUrl();
		Map<String,Object> config = new HashMap<>();
		config.put( Settings.URL, url );
		config.put( Settings.USER, "bogus" );
		config.put( Settings.PASS, "bogus" );
		ReactiveConnectionPool reactivePool = configureAndStartPool( config );
		test( context, assertThrown( PgException.class, verifyConnectivity( context, reactivePool ) )
				.thenAccept( e -> assertThat( e.getMessage() ).contains( "bogus" ) )
		);
	}

	private CompletionStage<Void> verifyConnectivity(VertxTestContext context, ReactiveConnectionPool reactivePool) {
		return reactivePool.getConnection().thenCompose(
				connection -> connection.select( "SELECT 1" )
						.thenAccept( rows -> {
							assertNotNull( rows );
							assertEquals( 1, rows.size() );
							assertEquals( 1, rows.next()[0] );
						} ) );
	}
}
