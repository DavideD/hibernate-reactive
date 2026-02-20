/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.reactive.context.Context;
import org.hibernate.reactive.context.impl.BaseKey;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.impl.MutinySessionImpl;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.ReactiveTransactionCoordinator;
import org.hibernate.reactive.pool.impl.ContextualConnectionPool;
import org.hibernate.reactive.pool.impl.DefaultSqlClientPool;
import org.hibernate.reactive.pool.impl.ExternalTransactionCoordinator;
import org.hibernate.reactive.pool.impl.ExternallyManagedConnection;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;
import static org.hibernate.reactive.util.impl.CompletionStages.voidFuture;

/**
 * Tests for transaction coordination abstraction when using an externally-managed transaction
 * coordination to ensure frameworks like Quarkus can properly integrate.
 *
 * @see NoLiveTransactionValidationErrorTest
 */
public class ExternalTransactionCoordinatorTest extends BaseReactiveTest {

	@Override
	protected Collection<Class<?>> annotatedEntities() {
		return List.of( Comic.class );
	}

	@Override
	protected void addServices(StandardServiceRegistryBuilder builder) {
		builder.addInitiator( new TestExternalConnectionPoolInitiator() );
	}

	@Test
	public void validationOnSessionClose(VertxTestContext context) {
		final Comic beneath = new Comic( "979-8887241081", "Beneath The Trees Where Nobody Sees" );
		ContextualConnectionPool pool = (ContextualConnectionPool) factoryManager.getReactiveConnectionPool();

		test(
				context,
				// External transaction manager gets connection and begins transaction
				pool.getConnection()
						.thenCompose( connection -> {
							ExternallyManagedConnection externalConnection = (ExternallyManagedConnection) connection;
							return externalConnection.beginTransaction()
									.thenCompose( v -> {
										assertThat( externalConnection.getTransactionCoordinator().isExternallyManaged() )
												.as( "Should use externally-managed coordinator" )
												.isTrue();

										return getMutinySessionFactory()
												.openSession()
												.chain( session -> {
													ReactiveConnection sessionConnection = ( (MutinySessionImpl) session ).getReactiveConnection();
													assertThat( sessionConnection )
															.as( "Session should use the same externally-managed connection" )
															.isSameAs( externalConnection );

													return doSomethingInTransaction( session, beneath );
												} )
												.subscribeAsCompletionStage();
									} )
									// External manager commits (triggers before-commit actions like flush)
									.thenCompose( vv -> externalConnection.commitTransaction() )
									// External manager closes the underlying connection and clears context
									.thenCompose( vv -> pool.closeConnection() );
						} )
						.thenCompose( v -> getSessionFactory()
								.withTransaction( s -> s.find( Comic.class, beneath.isbn ) )
						)
						.thenAccept( result -> assertThat( result )
								.as( "The entity should have been persisted in the database" )
								.isEqualTo( beneath )
						)
		);
	}

	@Test
	public void validationOnProxySessionClose(VertxTestContext context) {
		final Comic beneath = new Comic( "979-8887241081", "Beneath The Trees Where Nobody Sees" );
		ContextualConnectionPool pool = (ContextualConnectionPool) factoryManager.getReactiveConnectionPool();
		// External transaction manager gets connection and begins transaction
		ExternallyManagedConnection externalConnection = (ExternallyManagedConnection) pool.getProxyConnection();
		test(
				context,
				externalConnection
						.beginTransaction()
						.thenCompose( v -> {
							assertThat( externalConnection.getTransactionCoordinator().isExternallyManaged() )
									.as( "Should use externally-managed coordinator" )
									.isTrue();

							Mutiny.Session session = getMutinySessionFactory().createSession();
							ReactiveConnection sessionConnection = ( (MutinySessionImpl) session ).getReactiveConnection();
							assertThat( sessionConnection )
									.as( "Session should use the same externally-managed connection" )
									.isSameAs( externalConnection );

							return doSomethingInTransaction( session, beneath )
									.subscribeAsCompletionStage();
						} )
						// External manager commits (triggers before-commit actions like flush)
						.thenCompose( vv -> externalConnection.commitTransaction() )
						// External manager closes the underlying connection and clears context
						.thenCompose( vv -> pool.closeConnection() )
						.thenCompose( v -> getSessionFactory()
								.withTransaction( s -> s.find( Comic.class, beneath.isbn ) )
						)
						.thenAccept( result -> assertThat( result )
								.as( "The entity should have been persisted in the database" )
								.isEqualTo( beneath )
						)
		);
	}

	/**
	 * In Quarkus, a user would define the transaction boundaries using annotations ({@code @Transactional} or similar).
	 * The method itself is unaware of the transaction boundaries: it just inject a session via CDI and use it.
	 */
	private Uni<Void> doSomethingInTransaction(Mutiny.Session session, Comic beneath) {
		return session
				.persist( beneath )
				// Having a transaction active shouldn't cause any validation error
				// when we close the session. It will actually do nothing until the transaction completes.
				.call( session::close );
	}

	/**
	 * Service initiator for the test connection pool.
	 */
	static class TestExternalConnectionPoolInitiator implements org.hibernate.boot.registry.StandardServiceInitiator<ReactiveConnectionPool> {
		@Override
		public ReactiveConnectionPool initiateService(Map configurationValues, org.hibernate.service.spi.ServiceRegistryImplementor registry) {
			return new ContextualConnectionPool();
		}

		@Override
		public Class<ReactiveConnectionPool> getServiceInitiated() {
			return ReactiveConnectionPool.class;
		}
	}

	/**
	 * A connection pool that stores connections in the Vert.x context for
	 * testing the external transaction management.
	 * <p>
	 * This pool is designed for frameworks like Quarkus that manage transactions
	 * externally (similar to JTA). It:
	 * <ul>
	 *     <li>Stores connections in the Vert.x {@link Context}</li>
	 *     <li>Returns the same connection for all requests within the same context</li>
	 *     <li>Wraps connections with {@link ExternallyManagedConnection} for proper
	 *         transaction coordination</li>
	 *     <li>Uses {@link ExternalTransactionCoordinator} to handle transaction
	 *         synchronization callbacks</li>
	 * </ul>
	 *
	 * @see ExternalTransactionCoordinator
	 * @see ExternallyManagedConnection
	 */
	@Incubating
	public static class ContextualConnectionPool extends DefaultSqlClientPool {

		private static final Context.Key<ExternallyManagedConnection> CONNECTION_KEY =
				new BaseKey<>( ExternallyManagedConnection.class, "org.hibernate.reactive.external.connection" );

		private Context context;
		private final ExternalTransactionCoordinator coordinator;

		public ContextualConnectionPool() {
			this.coordinator = new ExternalTransactionCoordinator( this::getConnectionFromContext );
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			super.injectServices( serviceRegistry );
			this.context = serviceRegistry.getService( Context.class );
		}

		/**
		 * Returns the key used to store connections in the context.
		 * Subclasses can override to provide a custom key.
		 *
		 * @return the context key for connections
		 */
		protected Context.Key<ExternallyManagedConnection> getConnectionKey() {
			return CONNECTION_KEY;
		}

		@Override
		public ReactiveTransactionCoordinator getTransactionCoordinator() {
			return coordinator;
		}

		/**
		 * Gets the connection from the context, if one exists.
		 *
		 * @return the connection from context, or {@code null} if none exists
		 */
		private CompletionStage<ReactiveConnection> getConnectionFromContext() {
			ExternallyManagedConnection connection = context.get( getConnectionKey() );
			return connection != null ? completedFuture( connection ) : null;
		}

		/**
		 * Closes the underlying connection and removes it from context.
		 * Called by the external transaction manager after commit/rollback.
		 *
		 * @return a completion stage that completes when the connection is closed
		 */
		public CompletionStage<Void> closeConnection() {
			ExternallyManagedConnection connection = context.get( getConnectionKey() );
			context.remove( getConnectionKey() );
			return connection != null ? connection.closeUnderlyingConnection() : voidFuture();
		}

		@Override
		public CompletionStage<Void> getCloseFuture() {
			return closeConnection()
					.thenCompose( v -> super.getCloseFuture() );
		}

		@Override
		public CompletionStage<ReactiveConnection> getConnection() {
			// Check context first - return existing connection if present
			ExternallyManagedConnection existing = context.get( getConnectionKey() );
			if ( existing != null ) {
				return completedFuture( existing );
			}

			// No existing connection - create, wrap, and store in context
			return super.getConnection()
					.thenApply( connection -> {
						ExternallyManagedConnection wrapped = ExternallyManagedConnection.wrap( connection, coordinator );
						context.put( getConnectionKey(), wrapped );
						return wrapped;
					} );
		}

		@Override
		public ReactiveConnection getProxyConnection() {
			// Check context first - return existing connection if present
			ExternallyManagedConnection existing = context.get( getConnectionKey() );
			if ( existing != null ) {
				return existing;
			}
			ReactiveConnection proxyConnection = super.getProxyConnection();
			// No existing connection - create, wrap, and store in context
			ExternallyManagedConnection wrapped = ExternallyManagedConnection.wrap( proxyConnection, coordinator );
			context.put( getConnectionKey(), wrapped );
			return wrapped;
		}
	}

	@Entity
	public static class Comic {
		@Id
		public String isbn;
		public String title;

		public Comic() {
		}

		public Comic(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}

		@Override
		public String toString() {
			return isbn + ":" + title;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Comic comic = (Comic) o;
			return Objects.equals( isbn, comic.isbn ) && Objects.equals( title, comic.title );
		}

		@Override
		public int hashCode() {
			return Objects.hash( isbn, title );
		}
	}
}
