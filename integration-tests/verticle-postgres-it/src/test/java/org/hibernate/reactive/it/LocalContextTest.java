/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.reactive.it.verticle.Product;
import org.hibernate.reactive.it.verticle.ProductVerticle;
import org.hibernate.reactive.it.verticle.StartVerticle;
import org.hibernate.reactive.mutiny.Mutiny;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import junit.framework.AssertionFailedError;

import io.smallrye.mutiny.Uni;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;

import static java.util.concurrent.CompletableFuture.allOf;

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
public class LocalContextTest {

	// Number of requests: each request is a product created and then searched
	private static final int REQUEST_NUMBER = 20;

	@Rule
	public Timeout rule = Timeout.seconds( 5 * 60 );

	@Test
	public void testProductsGeneration(TestContext context) {
		final Async async = context.async();
		final Vertx vertx = Vertx.vertx( StartVerticle.vertxOptions() );

		Mutiny.SessionFactory sf = StartVerticle.createHibernateSessionFactory( StartVerticle.USE_DOCKER )
				.unwrap( Mutiny.SessionFactory.class );

		final WebClient webClient = WebClient.create( vertx.getDelegate() );

		final DeploymentOptions deploymentOptions = new DeploymentOptions();
		deploymentOptions.setInstances( 8 );

		vertx
				.deployVerticle( () -> new ProductVerticle( () -> sf ), deploymentOptions )
				.map( s -> webClient )
				.call( this::createProducts )
				.call( this::findProducts )
				.eventually( vertx::close )
				.subscribe().with(
						res -> async.complete(),
						context::fail
				);
	}

	/**
	 * Create several products using http requests
	 *
	 * @see #REQUEST_NUMBER
	 */
	private Uni<Void> createProducts(WebClient webClient) {
		CompletableFuture<?>[] postRequests = new CompletableFuture[REQUEST_NUMBER];
		for ( int i = 0; i < postRequests.length; i++ ) {
			Product product = new Product( i + 1 );

			Map<String, Object> properties = new HashMap<>();
			properties.put( "id", product.getId() );
			properties.put( "name", product.getName() );
			properties.put( "price", product.getPrice() );

			final Future<HttpResponse<Buffer>> send = webClient
					.post( ProductVerticle.HTTP_PORT, "localhost", "/products" )
					.sendJsonObject( new JsonObject( properties ) );
			postRequests[i] = send.toCompletionStage().toCompletableFuture();
		}
		return Uni.createFrom().completionStage( allOf( postRequests ) );
	}

	/**
	 * Use http requests to find the products previously created and validate them
	 *
	 * @see #REQUEST_NUMBER
	 */
	private Uni<Void> findProducts(WebClient webClient) {
		CompletableFuture<?>[] getRequests = new CompletableFuture[REQUEST_NUMBER];
		for ( int i = 0; i < getRequests.length; i++ ) {
			final CompletableFuture<Void> responseFuture = new CompletableFuture<>();
			getRequests[i] = responseFuture;
			final Product expected = new Product( i + 1 );

			// Send the request
			webClient.get( ProductVerticle.HTTP_PORT, "localhost", "/products/" + ( i + 1 ) )
					.send()
					.onComplete( event -> handle( expected, responseFuture, event ) );
		}
		return Uni.createFrom().completionStage( allOf( getRequests ) );
	}

	/**
	 * Check that the expected product is returned by the response.
	 */
	private void handle(Product expected, CompletableFuture<Void> future, AsyncResult<HttpResponse<Buffer>> event) {
		if ( event.succeeded() ) {
			if ( event.result().statusCode() != 200 ) {
				future.completeExceptionally( new AssertionFailedError( "Expected status code 200 but was " + event.result()
						.statusCode() ) );
			}
			else {
				final Product found = event.result().bodyAsJson( Product.class );
				if ( expected.equals( found ) ) {
					future.complete( null );
				}
				else {
					future.completeExceptionally( new AssertionFailedError( "Wrong value returned. Expected " + expected + " but was " + found ) );
				}
			}
		}
		else {
			future.completeExceptionally( event.cause() );
		}
	}
}
