/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.schema;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.reactive.containers.DatabaseConfiguration;
import org.hibernate.reactive.provider.Settings;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import static org.hibernate.reactive.BaseReactiveTest.test;
import static org.hibernate.reactive.util.impl.CompletionStages.*;

@RunWith(VertxUnitRunner.class)
public class JPASchemaUpdateTest {

	@Rule
	public Timeout rule = Timeout.seconds( 5 * 60 );

	private Properties properties;

	@ClassRule
	public static RunTestOnContext vertxContextRule = new RunTestOnContext( () -> {
		VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckInterval( 5 );
		options.setBlockedThreadCheckIntervalUnit( TimeUnit.MINUTES );
		return Vertx.vertx( options );
	} );

	private static void closeFactory(EntityManagerFactory sessionFactory, Throwable throwable) {
		try {
			sessionFactory.close();
		}
		catch (Throwable t) {
			System.out.println( t.getMessage() );
		}
	}

	@Before
	public void before() {
		properties = new Properties();
		properties.setProperty( Settings.URL, DatabaseConfiguration.getJdbcUrl() );
		properties.setProperty( Settings.USER, DatabaseConfiguration.USERNAME );
		properties.setProperty( Settings.PASS, DatabaseConfiguration.PASSWORD );
		properties.setProperty( Settings.SHOW_SQL, "true" );
		properties.setProperty( Settings.HBM2DDL_AUTO, "update" );
	}

	@Test
	public void testFactoryCreation(TestContext context) {
		test( context, voidFuture()
//				.thenApply( this::createEMFactory )
				.thenCompose( v -> vertxContextRule.vertx()
						.executeBlocking( this::createEMFactory )
						.toCompletionStage() )
				.whenComplete( JPASchemaUpdateTest::closeFactory ) );
	}

	private void createEMFactory(Promise<EntityManagerFactory> promise) {
		try {
			promise.complete( createEMF() );
		}
		catch (Throwable t) {
			promise.fail( t );
		}
	}

//	private EntityManagerFactory createEMFactory(Void ignore) {
//		return createEMFactory();
//	}

	private EntityManagerFactory createEMF() {
		return Persistence.createEntityManagerFactory( "pg-schema-update-test", properties );
	}
}
