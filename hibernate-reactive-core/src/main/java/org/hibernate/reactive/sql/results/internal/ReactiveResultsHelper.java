/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @see org.hibernate.sql.results.internal.ResultsHelper
 */
public class ReactiveResultsHelper {

	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			RowTransformer<R> rowTransformer,
			Class<R> transformedResultJavaType,
			JdbcValues jdbcValues) {
		final JdbcValuesMappingResolution jdbcValuesMappingResolution = jdbcValues
				.getValuesMapping().resolveAssemblers( sessionFactory );
		return new ReactiveStandardRowReader<>( jdbcValuesMappingResolution, rowTransformer, transformedResultJavaType );
	}
}
