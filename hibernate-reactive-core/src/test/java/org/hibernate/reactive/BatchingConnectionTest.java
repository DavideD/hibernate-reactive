/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.reactive.mutiny.impl.MutinySessionImpl;
import org.hibernate.reactive.mutiny.impl.MutinyStatelessSessionImpl;
import org.hibernate.reactive.pool.BatchingConnection;
import org.hibernate.reactive.pool.impl.SqlClientConnection;
import org.hibernate.reactive.stage.impl.StageSessionImpl;
import org.hibernate.reactive.stage.impl.StageStatelessSessionImpl;

import org.junit.Test;

import io.vertx.ext.unit.TestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.reactive.util.impl.CompletionStages.loop;

public class BatchingConnectionTest extends ReactiveSessionTest {

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, "5");
		return configuration;
	}

	@Test
	public void testBatching(TestContext context) {
		test( context, getSessionFactory()
				.withTransaction( s -> s
						.persist( new GuineaPig( 11, "One" ), new GuineaPig( 22, "Two" ), new GuineaPig( 33, "Three" ) )
						.thenCompose( v -> s.createQuery( "select count(*) from GuineaPig" )
								.getSingleResult()
								.thenAccept( count -> context.assertEquals( 3L, count ) ) ) )
				.thenCompose( v -> getSessionFactory()
						.withTransaction( s -> s
								.<GuineaPig>createQuery( "from GuineaPig" ).getResultList()
								.thenAccept( list -> list.forEach( pig -> pig.setName( "Zero" ) ) )
								.thenCompose( vv -> s.<Long>createQuery( "select count(*) from GuineaPig where name='Zero'" )
										.getSingleResult()
										.thenAccept( count -> context.assertEquals( 3L, count ) ) ) ) )
				.thenCompose( v -> getSessionFactory()
						.withTransaction( s -> s
								.<GuineaPig>createQuery( "from GuineaPig" )
								.getResultList()
								.thenCompose( list -> loop( list, s::remove ) )
								.thenCompose( vv -> s.<Long>createQuery( "select count(*) from GuineaPig" )
										.getSingleResult()
										.thenAccept( count -> context.assertEquals( 0L, count ) ) ) ) )
		);
	}

	@Test
	public void testBatchingConnection(TestContext context) {
		test( context, openSession()
				.thenAccept( session -> assertThat( ( (StageSessionImpl) session ).getReactiveConnection() )
						.isInstanceOf( BatchingConnection.class ) )
		);
	}

	@Test
	public void testBatchingConnectionWithStateless(TestContext context) {
		test( context, openStatelessSession()
				.thenAccept( session -> assertThat( ( (StageStatelessSessionImpl) session ).getReactiveConnection() )
						// Stateless session is not affected by the STATEMENT_BATCH_SIZE property
						.isInstanceOf( SqlClientConnection.class ) )
		);
	}

	@Test
	public void testBatchingConnectionMutiny(TestContext context) {
		test( context, openMutinySession()
				.invoke( session -> assertThat( ( (MutinySessionImpl) session ).getReactiveConnection() )
						.isInstanceOf( BatchingConnection.class ) )
		);
	}

	@Test
	public void testBatchingConnectionWithMutinyStateless(TestContext context) {
		test( context, openMutinyStatelessSession()
				.invoke( session -> assertThat( ( (MutinyStatelessSessionImpl) session ).getReactiveConnection() )
						// Stateless session is not affected by the STATEMENT_BATCH_SIZE property
						.isInstanceOf( SqlClientConnection.class ) )
		);
	}
}
