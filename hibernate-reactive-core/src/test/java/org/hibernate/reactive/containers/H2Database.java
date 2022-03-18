/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.containers;


import java.util.Map;


public class H2Database implements TestableDatabase {
	public static H2Database INSTANCE = new H2Database();

	@Override
	public String getJdbcUrl() {
		return "jdbc:h2:~/test";
	}

	@Override
	public String getUri() {
		return "h2:~/test";
	}


	@Override
	public String getScheme() {
		return "h2:";
	}

	@Override
	public String getNativeDatatypeQuery(String tableName, String columnName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getExpectedNativeDatatype(Class<?> dataType) {
		return null;
	}

	@Override
	public String createJdbcUrl(String host, int port, String database, Map<String, String> params) {
		// FIXME: Host and port might make sense if H2 is started with the right options
		return TestableDatabase.super.createJdbcUrl( "~", -1, database, params );
	}

	@Override
	public String jdbcStartQuery() {
		return ";";
	}

	@Override
	public String jdbcParamDelimiter() {
		return ";";
	}

	private H2Database() {
	}
}
