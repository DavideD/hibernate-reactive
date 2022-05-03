/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.hibernate.cfg.Configuration;

import org.junit.Before;
import org.junit.ClassRule;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;

public abstract class WithVertxContextTest extends BaseReactiveTest {

	@ClassRule
	public static RunTestOnContext vertxContextRule = new RunTestOnContext( () -> {
		VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckInterval( 5 );
		options.setBlockedThreadCheckIntervalUnit( TimeUnit.MINUTES );
		return Vertx.vertx( options );
	} );

	@Before
	public void before(TestContext context) {
		test( context, setupSessionFactory( constructConfiguration() ) );
	}

	protected CompletionStage<Void> setupSessionFactory(Configuration configuration) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		vertxContextRule.vertx()
				.executeBlocking(
						// schema generation is a blocking operation and so it causes an
						// exception when run on the Vert.x event loop. So call it using
						// Vertx.executeBlocking()
						promise -> startFactoryManager( promise, configuration ),
						event -> {
							if ( event.succeeded() ) {
								future.complete( null );
							}
							else {
								future.completeExceptionally( event.cause() );
							}
						}
				);
		return future;
	}

	protected void startFactoryManager(Promise<Object> p, Configuration configuration ) {
		try {
			factoryManager.start( () -> createHibernateSessionFactory( configuration ) );
			p.complete();
		}
		catch (Throwable e) {
			p.fail( e );
		}
	}
}
