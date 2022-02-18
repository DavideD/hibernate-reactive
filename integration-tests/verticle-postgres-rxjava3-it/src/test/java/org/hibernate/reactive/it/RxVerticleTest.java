/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.reactive.it.verticle.ProductVerticle;
import org.hibernate.reactive.it.verticle.StartVerticle;
import org.hibernate.reactive.stage.Stage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;

import static org.hibernate.reactive.it.verticle.StartVerticle.USE_DOCKER;

/**
 * Create a certain number of entities and check that they can be found using
 * the Vert.x web client.
 * <p>
 * The actual purpose of this test is to make sure that multiple parallel
 * http requests don't share a session.
 * </p>
 * <p>
 * Note that this test relies on the fact that we are making several requests
 * at the same time and it will fail if a http request closes the wrong session
 * because of an exception.
 * Theoretically, everything could happen in the right order because of chance
 * but it's unlikely and at the moment I don't have a better solution.
 * See the <a href="https://github.com/hibernate/hibernate-reactive/issues/1073">the related issue</a>
 * for more details.
 * <p>
 */
@RunWith(VertxUnitRunner.class)
public class RxVerticleTest {

	// Number of requests: each request is a product created and then searched
	private static final int REQUEST_NUMBER = 100000;

	@Rule
	public Timeout rule = Timeout.seconds( 5 * 60 );

	@Test
	public void testProductsGeneration(TestContext context) {
		final Async async = context.async();
		final Vertx vertx = Vertx.vertx( StartVerticle.vertxOptions() );
		final Stage.SessionFactory emf = StartVerticle.createHibernateSessionFactory( USE_DOCKER, vertx )
				.unwrap( Stage.SessionFactory.class );
		final WebClient webClient = WebClient.create( vertx );

		DeploymentOptions options = new DeploymentOptions();
		options.setInstances( 1 );

		vertx
				.deployVerticle( () -> new ProductVerticle( () -> emf ), options )
				.map( s -> webClient )
				.compose( this::findProducts )
				.onFailure( context::fail )
				.onSuccess( event -> async.complete() )
				.eventually( unused -> vertx.close() );
	}

	/**
	 * Use http requests to find the products previously created and validate them
	 *
	 * @see #REQUEST_NUMBER
	 */
	private Future<?> findProducts(WebClient webClient) {
		List<Future> getRequests = new ArrayList<>();
		for ( int i = 0; i < REQUEST_NUMBER; i++ ) {
			// Send the request
			final Future<?> getRequest = webClient
					.get( ProductVerticle.HTTP_PORT, "localhost", "/products" )
					.send();

			getRequests.add( getRequest );
		}
		return CompositeFuture.all( getRequests );
	}
}
