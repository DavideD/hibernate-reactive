/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.it.verticle;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.hibernate.reactive.stage.Stage;

import org.jboss.logging.Logger;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.Promise;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;

public class ProductVerticle extends AbstractVerticle {

	private static final Logger LOG = Logger.getLogger( ProductVerticle.class );

	private final Supplier<Stage.SessionFactory> emfSupplier;
	private Stage.SessionFactory emf;

	/**
	 * The port to use to listen to requests
	 */
	public static final int HTTP_PORT = 8088;

	public ProductVerticle(Supplier<Stage.SessionFactory> emfSupplier) {
		this.emfSupplier = emfSupplier;
	}

	private void startHibernate(Promise<Stage.SessionFactory> promise) {
		try {
			this.emf = emfSupplier.get();
			promise.complete( this.emf );
		}
		catch (Exception e) {
			promise.fail( e );
		}
	}

	@Override
	public Completable rxStart() {
		final Maybe<Stage.SessionFactory> sfMaybe = vertx
				.executeBlocking( this::startHibernate )
				.doOnComplete( () -> LOG.debug( "✅ Hibernate Reactive is ready" ) );

		Router router = Router.router( vertx );
		router.post().handler( BodyHandler.create() );
		router.get( "/products" ).respond( this::listProducts );
		router.get( "/products/:id" ).respond( this::getProduct );
		router.post( "/products" ).respond( this::createProduct );

		final Completable httpServerCompletable = vertx.createHttpServer()
				.requestHandler( router )
				.listen( HTTP_PORT )
				.doOnSuccess( s -> LOG.debugf( "✅ HTTP server listening on port $s", HTTP_PORT ) )
				.ignoreElement();

		return sfMaybe.concatMapCompletable( sessionFactory -> httpServerCompletable );
	}

	private Maybe<List<Product>> listProducts(RoutingContext ctx) {
		CompletionStage<List<Product>> stage = emf.withSession( session -> session
				.createQuery( "from Product", Product.class )
				.getResultList() );
		return Maybe.fromCompletionStage( stage );
	}

	private Maybe<Product> getProduct(RoutingContext ctx) {
		long id = Long.parseLong( ctx.pathParam( "id" ) );
		CompletionStage<Product> stage = emf.withSession( session -> session
				.find( Product.class, id ) );
		return Maybe.fromCompletionStage( stage );
	}

	private Maybe<Product> createProduct(RoutingContext ctx) {
		Product product = ctx.getBodyAsJson().mapTo( Product.class );
		CompletionStage<Product> stage = emf
				.withSession( session -> session.persist( product )
						.thenCompose( s -> session.flush() )
						.thenApply( unused -> product ) );
		return Completable.fromCompletionStage( stage ).toMaybe();
	}
}
