/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.pool.impl;

import java.util.Map;

import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.reactive.pool.ReactiveConnectionPool;
import org.hibernate.reactive.provider.Settings;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class H2ReactiveConnectionPoolInitiator extends ReactiveConnectionPoolInitiator {

	public static final H2ReactiveConnectionPoolInitiator INSTANCE = new H2ReactiveConnectionPoolInitiator();

	@Override
	public ReactiveConnectionPool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		Object configValue = configurationValues.get( Settings.SQL_CLIENT_POOL );
		String url = ConfigurationHelper.getString( Settings.URL, configurationValues );
		if ( configValue == null || url.startsWith( "jdbc:h2:" ) ) {
			return new H2SqlClientPool();
		}

		return super.initiateService( configurationValues, registry );
	}
}
