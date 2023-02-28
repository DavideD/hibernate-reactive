/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.sql.results.internal;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;
import org.hibernate.reactive.sql.results.spi.ReactiveValuesMappingProducer;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.internal.StandardJdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.reactive.util.impl.CompletionStages.completedFuture;

/**
 * @see org.hibernate.sql.results.jdbc.internal.JdbcValuesMappingProducerStandard
 */
public class ReactiveJdbcValuesMappingProducerStandard
		implements JdbcValuesMappingProducer, ReactiveValuesMappingProducer {

	private final JdbcValuesMapping resolvedMapping;

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ReactiveJdbcValuesMappingProducerStandard(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults) {
		this.resolvedMapping = new StandardJdbcValuesMapping( sqlSelections, domainResults );
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
	}

	@Override
	public JdbcValuesMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		final List<SqlSelection> sqlSelections = resolvedMapping.getSqlSelections();
		List<SqlSelection> resolvedSelections = null;
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			final SqlSelection sqlSelection = sqlSelections.get( i );
			final SqlSelection resolvedSelection = sqlSelection.resolve( jdbcResultsMetadata, sessionFactory );
			if ( resolvedSelection != sqlSelection ) {
				if ( resolvedSelections == null ) {
					resolvedSelections = new ArrayList<>( sqlSelections );
				}
				resolvedSelections.set( i, resolvedSelection );
			}
		}
		if ( resolvedSelections == null ) {
			return resolvedMapping;
		}
		return new StandardJdbcValuesMapping( resolvedSelections, resolvedMapping.getDomainResults() );
	}

	@Override
	public CompletionStage<JdbcValuesMapping> reactiveResolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		return completedFuture( resolve( jdbcResultsMetadata, sessionFactory ) );
	}
}
