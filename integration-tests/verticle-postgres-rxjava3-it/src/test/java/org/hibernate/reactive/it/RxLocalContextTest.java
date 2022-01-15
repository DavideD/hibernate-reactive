/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.it.verticle.Product;
import org.hibernate.reactive.it.verticle.ProductVerticle;
import org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.stage.Stage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import junit.framework.AssertionFailedError;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;

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
public class RxLocalContextTest {

	// These properties are in DatabaseConfiguration in core
	public static final boolean USE_DOCKER = Boolean.getBoolean( "docker" );
	public static final String IMAGE_NAME = "postgres:14.1";
	public static final String USERNAME = "hreact";
	public static final String PASSWORD = "hreact";
	public static final String DB_NAME = "hreact";

	// Number of requests: each request is a product created and then searched
	private static final int REQUEST_NUMBER = 20;

	@Rule
	public Timeout rule = Timeout.seconds( 5 * 60 );

	public static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>( IMAGE_NAME )
			.withUsername( USERNAME )
			.withPassword( PASSWORD )
			.withDatabaseName( DB_NAME )
			.withReuse( true );


	private VertxOptions vertxOptions() {
		VertxOptions vertxOptions = new VertxOptions();
		vertxOptions.setBlockedThreadCheckInterval( 5 );
		vertxOptions.setBlockedThreadCheckIntervalUnit( TimeUnit.MINUTES );
		return vertxOptions;
	}

	protected Configuration constructConfiguration() {
		Configuration configuration = new Configuration();
		configuration.addAnnotatedClass( Product.class );

		configuration.setProperty( Settings.HBM2DDL_AUTO, "create" );
		configuration.setProperty( Settings.URL, dbConnectionUrl() );
		configuration.setProperty( Settings.USER, USERNAME );
		configuration.setProperty( Settings.PASS, PASSWORD );

		//Use JAVA_TOOL_OPTIONS='-Dhibernate.show_sql=true'
		configuration.setProperty( Settings.SHOW_SQL, System.getProperty( Settings.SHOW_SQL, "false" ) );
		configuration.setProperty( Settings.FORMAT_SQL, System.getProperty( Settings.FORMAT_SQL, "false" ) );
		configuration.setProperty( Settings.HIGHLIGHT_SQL, System.getProperty( Settings.HIGHLIGHT_SQL, "true" ) );
		return configuration;
	}

	private String dbConnectionUrl() {
		if ( USE_DOCKER ) {
			// Calling start() will start the container (if not already started)
			// It is required to call start() before obtaining the JDBC URL because it will contain a randomized port
			postgresql.start();
			return postgresql.getJdbcUrl();
		}

		return "postgres://localhost:5432/" + DB_NAME;
	}

	private SessionFactory createHibernateSessionFactory(Configuration configuration) {
		StandardServiceRegistryBuilder builder = new ReactiveServiceRegistryBuilder()
				.applySettings( configuration.getProperties() );
		StandardServiceRegistry registry = builder.build();
		return configuration.buildSessionFactory( registry );
	}

	@Test
	public void testProductsGeneration(TestContext context) {
		final Async async = context.async();
		final Vertx vertx = Vertx.vertx( vertxOptions() );

		Supplier<Stage.SessionFactory> emfSupplier = () -> createHibernateSessionFactory( constructConfiguration() )
				.unwrap( Stage.SessionFactory.class );

		final WebClient webClient = WebClient.create( vertx );

		vertx
				.deployVerticle( () -> new ProductVerticle( emfSupplier ), new DeploymentOptions() )
				.map( s -> webClient )
				.compose( this::createProducts )
				.map( v -> webClient )
				.compose( this::findProducts )
				.onFailure( context::fail )
				.onSuccess( event -> async.complete() )
				.eventually( unused -> vertx.close() );
	}

	/**
	 * Create several products using http requests
	 *
	 * @see #REQUEST_NUMBER
	 */
	private Future<?> createProducts(WebClient webClient) {
		List<Future> postRequests = new ArrayList<>();
		for ( int i = 0; i < REQUEST_NUMBER; i++ ) {
			Product product = new Product( i + 1 );

			Map<String, Object> properties = new HashMap<>();
			properties.put( "id", product.getId() );
			properties.put( "name", product.getName() );
			properties.put( "price", product.getPrice() );

			final Future<HttpResponse<Buffer>> send = webClient
					.post( ProductVerticle.HTTP_PORT, "localhost", "/products" )
					.sendJsonObject( new JsonObject( properties ) );
			postRequests.add( send );
		}
		return CompositeFuture.all( postRequests );
	}

	/**
	 * Use http requests to find the products previously created and validate them
	 *
	 * @see #REQUEST_NUMBER
	 */
	private Future<?> findProducts(WebClient webClient) {
		List<Future> getRequests = new ArrayList<>();
		for ( int i = 0; i < REQUEST_NUMBER; i++ ) {
			final Product expected = new Product( i + 1 );

			// Send the request
			final Future<?> getRequest = webClient.get(
							ProductVerticle.HTTP_PORT,
							"localhost",
							"/products/" + expected.getId()
					)
					.send()
					.compose( event -> handle( expected, event ) );

			getRequests.add( getRequest );
		}
		return CompositeFuture.all( getRequests );
	}

	/**
	 * Check that the expected product is returned by the response.
	 */
	private Future<?> handle(Product expected, HttpResponse<Buffer> response) {
		if ( response.statusCode() != 200 ) {
			return Future.failedFuture( new AssertionFailedError( "Expected status code 200 but was " + response.statusCode() ) );
		}

		final Product found = response.bodyAsJson( Product.class );
		if ( !expected.equals( found ) ) {
			return Future.failedFuture( new AssertionFailedError( "Wrong value returned. Expected " + expected + " but was " + found ) );
		}

		return Future.succeededFuture();
	}
}
