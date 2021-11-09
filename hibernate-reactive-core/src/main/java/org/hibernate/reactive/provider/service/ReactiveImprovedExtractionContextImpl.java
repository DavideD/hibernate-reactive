/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.provider.service;

import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.pool.impl.Parameters;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.exec.ImprovedExtractionContextImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

import static org.hibernate.reactive.util.impl.CompletionStages.logSqlException;

public class ReactiveImprovedExtractionContextImpl extends ImprovedExtractionContextImpl {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ReactiveConnectionPool poolService;
	private final VertxInstance vertxService;

	public ReactiveImprovedExtractionContextImpl(
			ServiceRegistry registry,
			Identifier defaultCatalog,
			Identifier defaultSchema,
			DatabaseObjectAccess databaseObjectAccess) {
		super(
				registry,
				registry.getService( JdbcEnvironment.class ),
				NoopDdlTransactionIsolator.INSTANCE,
				defaultCatalog,
				defaultSchema,
				databaseObjectAccess
		);
		poolService = registry.getService( ReactiveConnectionPool.class );
		vertxService = registry.getService( VertxInstance.class );
	}

	@Override
	public <T> T getQueryResults(
			String queryString,
			Object[] positionalParameters,
			ResultSetProcessor<T> resultSetProcessor) throws SQLException {

		final Object[] parametersToUse = positionalParameters != null ? positionalParameters : new Object[0];
		final Parameters parametersDialectSpecific = Parameters.instance( getJdbcEnvironment().getDialect() );
		final String queryToUse = parametersDialectSpecific.process( queryString, parametersToUse.length );

		try {
			System.out.println( "----------------------------------------------------------------------------" );
			System.out.println( Thread.currentThread().getName() + ": getQueryResults thread: " );
			return queryResults( resultSetProcessor, parametersToUse, queryToUse ).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new HibernateException("Interrupted before receiving a result");
		}
		catch (ExecutionException e) {
			throw new HibernateException( e.getCause() );
		}
	}

	private <T> CompletableFuture<T> queryResults(
			ResultSetProcessor<T> resultSetProcessor,
			Object[] parametersToUse,
			String queryToUse) {
		final CompletableFuture<T> resultFuture = new CompletableFuture<>();
		final String launchingThread = Thread.currentThread().getName();
		System.out.println( launchingThread + ": Started" );
		poolService.getConnection()
				.thenApply( reactiveConnection -> {
					System.out.println( launchingThread + ": Connection acquired");
					return reactiveConnection;
				} )
				.thenCompose( connection -> connection
						.selectJdbcOutsideTransaction( queryToUse, parametersToUse )
						.whenComplete( (resultSet, throwable) -> logSqlException( throwable, () -> "could not execute query ", queryToUse ) )
						.thenApply( resultSet -> process( resultSetProcessor, resultSet ) )
						.handle( (result, throwable) -> connection.close()
								.handle( (v, tClosing) -> {
									System.out.println( launchingThread + ": Completed: " );
									if ( throwable != null ) {
										resultFuture.completeExceptionally( throwable );
									}
									else {
										resultFuture.complete( result );
									}
									return null;
								} )
						)
				)
				.exceptionally( throwable -> {
					// Just in case but should never happen
					resultFuture.completeExceptionally( throwable );
					return null;
				} );
		return resultFuture;
	}

	private <T> T process(ResultSetProcessor<T> resultSetProcessor, ResultSet resultSet) {
		try {
			return resultSetProcessor.process( resultSet );
		}
		catch (SQLException e) {
			throw new HibernateException( e );
		}
	}

	private static class NoopDdlTransactionIsolator implements DdlTransactionIsolator {
		static final NoopDdlTransactionIsolator INSTANCE = new NoopDdlTransactionIsolator();

		private NoopDdlTransactionIsolator() {
		}

		@Override
		public JdbcContext getJdbcContext() {
			return null;
		}

		@Override
		public void prepare() {
		}

		@Override
		public Connection getIsolatedConnection() {
			return NoopConnection.INSTANCE;
		}

		@Override
		public void release() {
		}
	}

	private static class NoopConnection implements Connection {

		static final NoopConnection INSTANCE = new NoopConnection();

		private NoopConnection() {
		}

		@Override
		public Statement createStatement() throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return null;
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return null;
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {

		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return false;
		}

		@Override
		public void commit() throws SQLException {

		}

		@Override
		public void rollback() throws SQLException {

		}

		@Override
		public void close() throws SQLException {

		}

		@Override
		public boolean isClosed() throws SQLException {
			return false;
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return null;
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {

		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return false;
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {

		}

		@Override
		public String getCatalog() throws SQLException {
			return null;
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {

		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return 0;
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return null;
		}

		@Override
		public void clearWarnings() throws SQLException {

		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return null;
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return null;
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

		}

		@Override
		public void setHoldability(int holdability) throws SQLException {

		}

		@Override
		public int getHoldability() throws SQLException {
			return 0;
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return null;
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return null;
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {

		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {

		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(
				String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(
				String sql,
				int resultSetType,
				int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return null;
		}

		@Override
		public Clob createClob() throws SQLException {
			return null;
		}

		@Override
		public Blob createBlob() throws SQLException {
			return null;
		}

		@Override
		public NClob createNClob() throws SQLException {
			return null;
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return null;
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return false;
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {

		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {

		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return null;
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return null;
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return null;
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return null;
		}

		@Override
		public void setSchema(String schema) throws SQLException {

		}

		@Override
		public String getSchema() throws SQLException {
			return null;
		}

		@Override
		public void abort(Executor executor) throws SQLException {

		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return 0;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}
	}
}
