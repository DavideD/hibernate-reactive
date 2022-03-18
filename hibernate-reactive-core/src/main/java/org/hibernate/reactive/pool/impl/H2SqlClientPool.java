/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.pool.impl;


import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.hibernate.HibernateError;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

public class H2SqlClientPool extends SqlClientPool
		implements ServiceRegistryAwareService, Configurable, Stoppable, Startable {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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

	public void configure(Map configuration) {
		uri = jdbcUrl( configuration );
	}

	public void start() {
		if ( pools == null ) {
			pools = createPool( uri );
		}
	}

	@Override
	public CompletionStage<Void> getCloseFuture() {
		return closeFuture.toCompletionStage();
	}

	@Override
	protected Pool getPool() {
		return pools;
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

	private URI jdbcUrl(Map<?, ?> configurationValues) {
		String url = ConfigurationHelper.getString( Settings.URL, configurationValues );
		LOG.sqlClientUrl( url );
		return parse( url );
	}

	public void stop() {
		if ( pools != null ) {
			this.closeFuture = pools.close();
		}
	}

	public static URI parse(String url) {

		if ( url == null || url.trim().isEmpty() ) {
			throw new HibernateError(
					"The configuration property '" + Settings.URL + "' was not provided, or is in invalid format. This is required when using the default DefaultSqlClientPool: " +
							"either provide the configuration setting or integrate with a different SqlClientPool implementation" );
		}

		if ( url.startsWith( "jdbc:" ) ) {
			return URI.create( url.substring( 5 ) );
		}

		return URI.create( url );
	}

	@Override
	protected SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}
}
