/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.pool.impl;

import java.net.URI;

import io.vertx.jdbcclient.JDBCConnectOptions;

public class H2ClientPoolConfiguration extends DefaultSqlClientPoolConfiguration
		implements JdbcClientPoolConfiguration {

	@Override
	public JDBCConnectOptions jdbcConnectOptions(URI uri) {

		return new JDBCConnectOptions()
				// H2 connection string
				.setJdbcUrl( uri.toString() )
				// username
				.setUser( getUser() == null ? "SA" : getUser() )
				// password
				.setPassword( getPassword() );
	}
}
