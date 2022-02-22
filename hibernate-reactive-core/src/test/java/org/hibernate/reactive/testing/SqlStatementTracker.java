/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Test utility to capture executed sql statement string values
 * <p>
 * Tests must instantiate this class with their desired filter and control bounds of the captured queries.
 */
public class SqlStatementTracker extends SqlStatementLogger {
	public static Predicate<String> INSERT_FILTER = s -> ( s.toLowerCase().startsWith( "insert" ) );
	public static Predicate<String> UPDATE_FILTER = s -> ( s.toLowerCase().startsWith( "update" ) );
	public static Predicate<String> DELETE_FILTER = s -> ( s.toLowerCase().startsWith( "delete" ) );
	public static Predicate<String> SELECT_FILTER = s -> ( s.toLowerCase().startsWith( "select" ) );

	private Predicate<String> filter;

	private List<String> sqlList = new ArrayList<>();
	private static SqlStatementTracker sqlTracker;
	private boolean showSql;

	public SqlStatementTracker(Predicate<String> predicate) {
		this.sqlTracker = this;
		this.filter = predicate;
	}

	public SqlStatementTracker(Predicate<String> predicate, boolean showSql, boolean formatSql, boolean hilightSql, long logSlowQuery) {
		super(showSql, formatSql, hilightSql, logSlowQuery);
		this.sqlTracker = this;
		this.filter = predicate;
	}

	@Override
	public void logStatement(String statement, Formatter formatter) {
		addSql( statement );
		super.logStatement( statement, formatter );
	}

	private void addSql(String sql) {
		if ( filter.test( sql ) ) {
			sqlList.add( sql );
		}
	}

	public List<String> getLoggedQueries() {
		return sqlList;
	}

	public void clear() {
		sqlList.clear();
	}

	public void registerService(StandardServiceRegistryBuilder builder) {
		builder.addInitiator( Initiator.INSTANCE );
	}

	static class Initiator implements StandardServiceInitiator<JdbcServices> {
		public static final Initiator INSTANCE = new Initiator();

		@Override
		public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			return new JdbcServicesLogger();
		}

		@Override
		public Class<JdbcServices> getServiceInitiated() {
			return JdbcServices.class;
		}
	}

	static class JdbcServicesLogger extends JdbcServicesImpl {
		@Override
		public SqlStatementLogger getSqlStatementLogger() {
			return sqlTracker;
		}
	}
}
