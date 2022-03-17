/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.pool.impl;


import java.net.URI;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.SqlConnection;

public class H2SqlClientPool extends SqlClientPool implements ServiceRegistryAwareService {

	//Asynchronous shutdown promise: we can't return it from #close as we implement a
	//blocking interface.
	private volatile Future<Void> closeFuture = Future.succeededFuture();

	private Pool pools;
	private URI uri;
	private SqlStatementLogger sqlStatementLogger;
	private ServiceRegistryImplementor serviceRegistry;

	public H2SqlClientPool() {
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		sqlStatementLogger = serviceRegistry.getService( JdbcServices.class ).getSqlStatementLogger();
	}

	public void start() {
		if ( pools == null ) {
			pools = createPool( uri );
		}
	}

	public void stop() {
		if ( pools != null ) {
			this.closeFuture = pools.close();
		}
	}

	private Pool createPool(URI uri) {
		SqlClientPoolConfiguration configuration = serviceRegistry.getService( SqlClientPoolConfiguration.class );
		VertxInstance vertx = serviceRegistry.getService( VertxInstance.class );

		return createPool( uri, configuration.connectOptions( uri ), configuration.poolOptions(), vertx.getVertx() );
	}

	private Pool createPool(URI uri, SqlConnectOptions connectOptions, PoolOptions poolOptions, Vertx vertx) {
		JDBCConnectOptions jdbcOptions = new JDBCConnectOptions();
		jdbcOptions.setUser( connectOptions.getUser() );
		jdbcOptions.setJdbcUrl( "jdbc:" + uri.toString() );
		JDBCPool pool = JDBCPool.pool( vertx, jdbcOptions, poolOptions );

		return pool;
	}

	@Override
	protected Pool getPool() {
		return pools;
	}

	@Override
	protected SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	@Override
	public CompletionStage<Void> getCloseFuture() {
		return closeFuture.toCompletionStage();
	}

	@Override
	public CompletionStage<ReactiveConnection> getConnection() {
		return getConnectionFromPool( getPool() );
	}

	@Override
	public CompletionStage<ReactiveConnection> getConnection(String tenantId) {
		return getConnectionFromPool( getTenantPool( tenantId ) );
	}

	private CompletionStage<ReactiveConnection> getConnectionFromPool(Pool pool) {
		start();
		return pool.getConnection().toCompletionStage().thenApply( this::newConnection );
	}

	private SqlClientConnection newConnection(SqlConnection connection) {
		return new SqlClientConnection( connection, getPool(), getSqlStatementLogger() );
	}
}
