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

	private static final String DEFAULT_URL = "jdbc:h2:~/hreact;DATABASE_TO_UPPER=FALSE";

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

	@Override
	public void configure(Map configuration) {
		uri = jdbcUrl( configuration );
	}

	@Override
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
		return createPool( uri, connectOptions( uri ), configuration.poolOptions(), vertx.getVertx() );
	}

	private Pool createPool(URI uri, SqlConnectOptions connectOptions, PoolOptions poolOptions, Vertx vertx) {
		JDBCConnectOptions jdbcOptions = new JDBCConnectOptions();
		jdbcOptions.setUser( connectOptions.getUser() );
		jdbcOptions.setPassword( connectOptions.getPassword() );
		// TODO: set other options based on URI parameters
		jdbcOptions.setJdbcUrl( uri.toString() );

		return JDBCPool.pool( vertx, jdbcOptions, poolOptions );
	}

	private URI jdbcUrl(Map<?, ?> configurationValues) {
		String url = ConfigurationHelper.getString( Settings.URL, configurationValues );
		LOG.sqlClientUrl( url );
		return parse( url );
	}

	private SqlConnectOptions connectOptions(URI uri) {
		// H2 separates parameters in the url with a semicolon (';')
		// "jdbc:h2:~/test;DATABASE_TO_UPPER=FALSE";
		SqlConnectOptions options = new SqlConnectOptions()
				// username
				.setUser( "sa" )
				// password
				.setPassword( "" );
		int index = uri.toString().indexOf( ';' );
		if ( index > -1 ) {
			String query = uri.toString().substring( index + 1 );
			String[] params = query.split( ";" );
			for ( String param : params ) {
				final int position = param.indexOf( "=" );
				if ( position != -1 ) {
					// We assume the first '=' is the one separating key and value
					// TODO: Check for specific JDBCConnectOptions and set based on available parameter values
					String key = param.substring( 0, position );
					String value = param.substring( position + 1 );
					options.addProperty( key, value );
				}
			}
		}
		return options;
	}

	@Override
	public void stop() {
		if ( pools != null ) {
			this.closeFuture = pools.close();
		}
	}

	public static URI parse(String url) {
		if ( url == null || url.trim().isEmpty() ) {
			return URI.create( DEFAULT_URL );
		}

		if ( url.startsWith( "jdbc:" ) ) {
			return URI.create( url );
		}

		return URI.create( "jdbc:" + url );
	}

	@Override
	protected SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}
}