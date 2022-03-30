/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.pool.impl;

import java.net.URI;

import io.vertx.jdbcclient.JDBCConnectOptions;

public class H2ClientPoolConfiguration extends DefaultSqlClientPoolConfiguration implements JdbcClientPoolConfiguration {

	@Override
	public JDBCConnectOptions jdbcConnectOptions(URI uri) {
		// TODO: We have to read the options from the uri and configuration
		JDBCConnectOptions jdbcOptions = new JDBCConnectOptions();
		jdbcOptions.setUser( getUser() == null ? "sa" : getUser() );
		jdbcOptions.setPassword( getPassword() );
		jdbcOptions.setJdbcUrl( uri.toString() );
		return jdbcOptions;
	}
}
