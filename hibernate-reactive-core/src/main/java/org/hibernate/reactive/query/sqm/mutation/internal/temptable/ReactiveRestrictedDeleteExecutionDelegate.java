/* Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.reactive.query.sqm.mutation.internal.temptable;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.RestrictedDeleteExecutionDelegate;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.reactive.logging.impl.Log;
import org.hibernate.reactive.logging.impl.LoggerFactory;

public class ReactiveRestrictedDeleteExecutionDelegate extends RestrictedDeleteExecutionDelegate
		implements ReactiveTableBasedDeleteHandler.ReactiveExecutionDelegate {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ReactiveRestrictedDeleteExecutionDelegate(
			EntityMappingType entityDescriptor,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings queryParameterBindings,
			SessionFactoryImplementor sessionFactory) {
		super(
				entityDescriptor,
				idTable,
				afterUseAction,
				sqmDelete,
				domainParameterXref,
				sessionUidAccess,
				queryOptions,
				loadQueryInfluencers,
				queryParameterBindings,
				sessionFactory
		);
	}

	@Override
	public CompletionStage<Integer> reactiveExecute(DomainQueryExecutionContext executionContext) {
		throw LOG.notYetImplemented();
	}
}
